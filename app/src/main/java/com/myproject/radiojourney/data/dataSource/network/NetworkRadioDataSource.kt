package com.myproject.radiojourney.data.dataSource.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import android.util.Log
import com.google.gson.JsonParseException
import com.myproject.radiojourney.data.dataSource.network.entity.CountryRemote
import com.myproject.radiojourney.data.dataSource.network.entity.RadioStationRemote
import com.myproject.radiojourney.data.dataSource.network.service.IRadioService
import com.myproject.radiojourney.data.dataSource.network.service.IRadioServiceWrapper
import com.myproject.radiojourney.other.Constants.DNS_ATTEMPTS
import com.myproject.radiojourney.other.Constants.DNS_RETRY_DELAY
import com.myproject.radiojourney.other.Constants.DNS_SERVER_LIST_NAME
import com.myproject.radiojourney.other.Constants.FALLBACK_SERVER
import com.myproject.radiojourney.other.Constants.MAX_STATIONS_COUNT
import com.myproject.radiojourney.other.Constants.SERVER_RETRY_DELAY
import com.myproject.radiojourney.other.Constants.SERVER_SEARCH_TIME
import com.myproject.radiojourney.other.Resource
import com.myproject.radiojourney.other.ServerError
import com.myproject.radiojourney.other.Status
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException

/**
 * Remote data source: все запросы к каталогу радиостанций radio-browser (список стран, станции страны,
 * отметка "станцию слушают").
 *
 * Автор API просит обращаться к серверам не по постоянному адресу, а так (https://api.radio-browser.info/):
 * 1. Взять список серверов DNS-запросом имени all.api.radio-browser.info (updateDNSList). Обратный DNS-запрос
 *    даёт имя сервера: по IP-адресу https-запрос не пройдёт, сертификат выдан на имя.
 *    Если DNS не ответил, берём сервер из документации (FALLBACK_SERVER).
 * 2. Перемешать список и ходить по нему по очереди, пока сервер не ответит (requestFromAnyServer),
 *    чтобы все приложения не нагружали один и тот же сервер. На сентябрь 2026 сервер в списке всё равно один - de1.
 * 3. Представляться заголовком User-Agent (UserAgentInterceptor).
 * 4. На каждый выбор станции пользователем отправлять /json/url/{uuid} - так каталог считает популярность станций
 *    (sendGetRequestToMarkRadioStationAsPopular, вызывается из HomeRadioFragment).
 *
 * Здесь же разбираются неудачи: почему запрос не удался (см. failureReason и ServerError) - от этого зависит,
 * что приложение скажет пользователю и стоит ли пробовать дальше
 */
class NetworkRadioDataSource @Inject constructor(
    private val radioServiceWrapper: IRadioServiceWrapper,
    @ApplicationContext private val context: Context
) : INetworkRadioDataSource {
    companion object {
        private const val TAG = "NetworkRadioDataSource"
    }

    override suspend fun getCountryList(): List<CountryRemote> =
        requestFromAnyServer(isValidResult = { it.isNotEmpty() }) { getCountryList() }.data
            ?: listOf()

    // Список станций страны. Серверы перебираются по кругу, пока не пройдёт SERVER_SEARCH_TIME
    // (полоса загрузки PROGRESS_TIMEOUT рассчитана так, чтобы не пропасть раньше, чем закончится перебор).
    // При ошибке в Resource.message - причина (ServerError), по ней экран выбирает, что написать пользователю
    override suspend fun getRadioStationList(
        countryCode: String,
        limit: Int
    ): Resource<List<RadioStationRemote>> {
        val response = requestFromAnyServer(
            // Полный список ищем долго: без него плеер останется без станций. Короткий запасной - один проход по серверам:
            // его просят сразу после долгой неудачи с полным, и пользователь уже подождал
            searchTimeMs = if (limit >= MAX_STATIONS_COUNT) SERVER_SEARCH_TIME else 0L,
            // Короткий список - по новому соединению: в сетях с ограничением "первые ~16-20 КБ соединения"
            // он не должен делить соединение с другими запросами (см. RadioServiceWrapper)
            freshConnection = limit < MAX_STATIONS_COUNT,
            isValidResult = { it.isNotEmpty() }
        ) {
            getRadioStationList(searchTerm = countryCode.uppercase(), limit = limit)
        }
        val radioStationRemoteList = response.data
            ?: return Resource.error(
                response.message ?: ServerError.SERVER_NOT_RESPONDING.name,
                listOf()
            )

        // !!! ПРОБЛЕМА:
        // Иногда получаем слишком длинный список радиостанций, из-за чего программа зависает.
        // РЕШЕНИЕ:
        // 1. Пагинация - не получилась, переходим к следующему варианту решения.
        // 2. Уменьшить количество станций:
        // - Упорядочить список по количеству прослушиваний.
        // - Затем оставить MAX_STATIONS_COUNT самых популярных.
        // - Упорядочить по алфавиту и затем передать как результат выполнение метода
        return try {
            val resultStationsRemote = radioStationRemoteList
                .asSequence()
                // Фильтрация невалидных станций. lastcheckok - на случай, если сервер не учёл hidebroken
                .filter { station -> station.name.isNotBlank() && station.url.isNotBlank() && station.lastcheckok == 1 }
                .sortedByDescending { it.clickcount }
                .take(MAX_STATIONS_COUNT)
                .sortedBy { station -> station.name.trim().lowercase() }
                .toList()

            if (resultStationsRemote.isEmpty()) Log.w(TAG, "No valid stations after filtering!")
            Resource.success(resultStationsRemote)
        } catch (e: RuntimeException) {
            // Например, у станции нет названия (null пришёл в не-null поле)
            Log.d(TAG, "Ошибка обработки списка станций: ${e.message}")
            Resource.error(ServerError.SERVER_NOT_RESPONDING.name, listOf())
        }
    }

    // Возвращает true, если ни один сервер не ответил
    override suspend fun sendGetRequestToMarkRadioStationAsPopular(stationUuid: String): Boolean =
        requestFromAnyServer(isValidResult = { it.ok != null }) {
            markStationAsPopular(stationUuid = stationUuid)
        }.status == Status.ERROR

    /**
     * Выполнить запрос на одном из серверов radio-browser и вернуть первый подходящий ответ.
     *
     * Раньше этот перебор серверов был написан трижды (для стран, для станций и для отметки популярности),
     * каждый раз чуть по-своему и с разной обработкой ошибок. Отличались же эти три места только двумя вещами:
     * каким методом API дёргать сервер и какой ответ считать удачным. Именно их метод и принимает параметрами.
     *
     * Как читать сигнатуру:
     * - `<T>` - тип ответа. Метод не знает, что именно он получает: список стран, список станций или ответ "ok".
     *   Тип подставит компилятор по лямбде [request] (для стран T = List<CountryRemote> и т.д.);
     * - [request] - `suspend IRadioService.() -> T` - это "лямбда с приёмником": внутри неё `this` - это готовый
     *   IRadioService нужного сервера, поэтому в вызове пишется просто `{ getCountryList() }`, без имени переменной.
     *   `suspend` - потому что внутри вызывается suspend-метод Retrofit;
     * - [isValidResult] - что считать удачей. Сервер может ответить 200 OK и прислать пустой список, а нам нужен
     *   следующий сервер. Для стран и станций это `{ it.isNotEmpty() }`, для отметки популярности - `{ it.ok != null }`;
     * - [searchTimeMs] - сколько времени перебирать серверы по кругу. 0 - обойти каждый сервер ровно один раз.
     *   Долго перебираем только плейлист станций (SERVER_SEARCH_TIME): без него пользователь увидит ошибку
     *   из-за одного неудачного сервера, а список стран и отметка популярности могут подождать до следующего запуска.
     * - [freshConnection] - каждую попытку делать по новому соединению, не переиспользуя открытые (см. RadioServiceWrapper).
     *
     * @return Resource.success с ответом первого сервера, для которого [isValidResult] вернул true,
     * или Resource.error, если не ответил никто. В message - причина неудачи (имя из [ServerError])
     */
    private suspend fun <T> requestFromAnyServer(
        searchTimeMs: Long = 0L,
        freshConnection: Boolean = false,
        isValidResult: (T) -> Boolean,
        request: suspend IRadioService.() -> T
    ): Resource<T> {
        // Шаг 1 документации API: получить список доступных серверов через DNS.
        // distinct() - у одного сервера несколько IP-адресов (IPv4 и IPv6), и DNS возвращает его имя столько раз,
        // сколько у него адресов. Без distinct() мы бы ходили на один и тот же сервер по два раза подряд.
        // Шаг 2 документации: перемешать список, чтобы все пользователи приложения не нагружали один и тот же сервер
        val servers = updateDNSList().distinct().shuffled()

        // elapsedRealtime() - время с момента загрузки телефона. В отличие от System.currentTimeMillis() оно не прыгнет,
        // если пользователь (или сеть) переведёт часы, поэтому для измерения длительности берут именно его
        val searchStartTime = SystemClock.elapsedRealtime()
        var attempt = 0

        // Что пошло не так. Если хоть раз ответ оборвался на середине, сообщаем именно об этом:
        // это самая полезная для пользователя подсказка (см. ServerError.CONNECTION_CUT)
        var failure = ServerError.SERVER_NOT_RESPONDING
        // Сколько попыток подряд ответ обрывался. Обрыв повторяется на каждой попытке, а каждая ждёт таймаута чтения,
        // поэтому, когда все серверы по разу (но не меньше двух раз) оборвали ответ, перебор прекращаем
        var cutsInRow = 0

        // Условие цикла читается так: пока есть куда ходить И (мы ещё не обошли каждый сервер по одному разу
        // ИЛИ нам разрешено ходить по кругу и время перебора ещё не вышло)
        while (servers.isNotEmpty() &&
            (attempt < servers.size || SystemClock.elapsedRealtime() - searchStartTime < searchTimeMs)
        ) {
            // Остаток от деления - это и есть "по кругу": когда attempt дойдёт до конца списка, снова начнём с нулевого сервера
            val baseURL = "https://${servers[attempt % servers.size]}"
            attempt++
            Log.d(TAG, "Запрос к серверу $baseURL, попытка $attempt")

            try {
                // Вот здесь вызывается лямбда: getRadioService(baseURL) даёт IRadioService этого сервера,
                // а .request() выполняет на нём тот метод API, который передали в параметре
                val result = radioServiceWrapper.getRadioService(baseURL, freshConnection).request()
                // Ответ получен. Если он нас устраивает - выходим из цикла и из метода, остальные серверы не трогаем
                if (isValidResult(result)) return Resource.success(result)
                Log.d(TAG, "Сервер $baseURL прислал пустой ответ")
                cutsInRow = 0
            } catch (e: CancellationException) {
                // Корутину отменили (например, пользователь выбрал другой плейлист, и старая загрузка больше не нужна).
                // Отмену нельзя "проглатывать" вместе с остальными ошибками: если её поймать и продолжить цикл,
                // мы будем ходить по серверам ради результата, который уже никому не нужен. Поэтому пробрасываем дальше
                throw e
            } catch (e: Exception) {
                // Любая ошибка этого сервера - не повод сдаваться, просто пробуем следующий:
                // IOException (нет сети, таймаут), HttpException (например, 502 или 503),
                // JsonSyntaxException (сервер жив, но ответил не тем, что мы ждём)
                val reason = failureReason(e)
                Log.d(TAG, "Сервер $baseURL: ${e.javaClass.simpleName}: ${e.message} -> $reason")
                if (reason == ServerError.CONNECTION_CUT) {
                    failure = ServerError.CONNECTION_CUT
                    cutsInRow++
                } else {
                    cutsInRow = 0
                }
            }

            // Телефон вообще не подключён к сети (режим полёта, выключены Wi-Fi и мобильные данные) - перебирать серверы
            // бессмысленно: каждый ответит той же ошибкой. Раньше пользователь ждал до конца SERVER_SEARCH_TIME
            if (!hasNetworkConnection()) {
                Log.d(TAG, "Нет подключения к сети - перебор серверов прекращаем")
                return Resource.error(ServerError.NO_NETWORK.name, null)
            }

            if (cutsInRow >= maxOf(2, servers.size)) {
                Log.d(TAG, "Ответ обрывается на каждом сервере - перебор прекращаем")
                break
            }

            // Пауза перед следующей попыткой - чтобы не завалить серверы запросами, если они отвечают ошибкой мгновенно.
            // Условие то же, что у цикла: паузу делаем, только если следующая попытка вообще будет.
            // delay() не занимает поток (в отличие от Thread.sleep) и умеет отменяться вместе с корутиной
            if (attempt < servers.size || searchTimeMs > 0) delay(SERVER_RETRY_DELAY)
        }

        // Никто не ответил. Что это значит для пользователя, решает вызывающий код:
        // для станций - диалог с причиной, для стран - пустой список
        return Resource.error(failure.name, null)
    }

    // На каком этапе сломался запрос - по типу ошибки.
    // - До ответа: не нашли сервер, не подключились, сервер ответил ошибкой (HttpException, например 503) -
    //   сервер не отвечает (или недоступен совсем).
    // - После начала ответа: соединение сбросили, данные перестали приходить (таймаут чтения), ответ кончился
    //   раньше времени (EOFException, "unexpected end of stream"), и Gson не смог разобрать обрезанный JSON -
    //   ответ оборвался. Так выглядит ограничение провайдера "первые 16 КБ, дальше обрыв".
    // Это догадка по косвенным признакам: медленный, но рабочий сервер тоже может не успеть до таймаута чтения.
    // Поэтому в тексте для пользователя "похоже" и "возможно", а не "точно"
    private fun failureReason(e: Exception): ServerError = when (e) {
        is UnknownHostException, is ConnectException, is NoRouteToHostException, is SSLHandshakeException, is HttpException ->
            ServerError.SERVER_NOT_RESPONDING
        // OkHttp пишет "failed to connect to ...", если не дождался подключения, и просто "timeout" / "Read timed out",
        // если не дождался данных уже после подключения
        is SocketTimeoutException ->
            if (e.message.orEmpty()
                    .startsWith("failed to connect")
            ) ServerError.SERVER_NOT_RESPONDING else ServerError.CONNECTION_CUT

        is IOException, is JsonParseException -> ServerError.CONNECTION_CUT
        else -> ServerError.SERVER_NOT_RESPONDING
    }

    // Есть ли у телефона подключение, через которое вообще можно выйти в интернет (Wi-Fi, мобильная сеть, Ethernet).
// Работает ли сам интернет, не проверяем: это и выясняет перебор серверов
    private fun hasNetworkConnection(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // do the DNS request
// suspend + withContext(IO): поиск DNS блокирует поток, а пауза между попытками - delay (не занимает поток, как Thread.sleep)
    private suspend fun updateDNSList(attemptsLeft: Int = DNS_ATTEMPTS): List<String> =
        withContext(Dispatchers.IO) {
            val listDNSResult = mutableListOf<String>()
            try {
                // add all round robin servers one by one to select them separately
                val list = InetAddress.getAllByName(DNS_SERVER_LIST_NAME)
                for (item in list) {
                    // canonicalHostName делает обратный DNS-запрос. Если он не удался, вместо имени сервера возвращается IP-адрес,
                    // а https-запрос по IP не пройдёт (сертификат выдан на имя) - такие результаты пропускаем
                    val hostName = item.canonicalHostName
                    if (hostName != item.hostAddress) listDNSResult.add(hostName)
                }
            } catch (e: UnknownHostException) {
                Log.d(TAG, "DNS: ${e.message}")
            }
            Log.d(TAG, "Серверы из DNS: $listDNSResult")

            // Без интернета DNS не отвечает: несколько попыток, а потом сервер, известный из документации radio-browser
            if (listDNSResult.isNotEmpty()) {
                listDNSResult
            } else if (attemptsLeft > 1 && hasNetworkConnection()) { // без сети повторять DNS-запрос незачем
                delay(DNS_RETRY_DELAY)
                updateDNSList(attemptsLeft - 1)
            } else {
                Log.d(TAG, "Список серверов не получен, используем $FALLBACK_SERVER")
                listOf(FALLBACK_SERVER)
            }
        }
}