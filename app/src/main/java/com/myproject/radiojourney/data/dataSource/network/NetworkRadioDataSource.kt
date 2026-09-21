package com.myproject.radiojourney.data.dataSource.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import android.util.Log
import com.myproject.radiojourney.data.dataSource.network.entity.CountryRemote
import com.myproject.radiojourney.data.dataSource.network.entity.RadioStationRemote
import com.myproject.radiojourney.data.dataSource.network.service.IRadioService
import com.myproject.radiojourney.data.dataSource.network.service.IRadioServiceWrapper
import com.myproject.radiojourney.other.Constants.DNS_ATTEMPTS
import com.myproject.radiojourney.other.Constants.DNS_RETRY_DELAY
import com.myproject.radiojourney.other.Constants.DNS_SERVER_LIST_NAME
import com.myproject.radiojourney.other.Constants.FALLBACK_SERVER
import com.myproject.radiojourney.other.Constants.MAX_STATIONS_COUNT
import com.myproject.radiojourney.other.Constants.SERVER_IS_DOWN
import com.myproject.radiojourney.other.Constants.SERVER_RETRY_DELAY
import com.myproject.radiojourney.other.Constants.SERVER_SEARCH_TIME
import com.myproject.radiojourney.other.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.UnknownHostException
import javax.inject.Inject

/**
 * These steps should be done in your APP or program.
 * 1. Get a list of available servers
 * Do a DNS-lookup of 'all.api.radio-browser.info'. This gives you a list of all available servers. To get the nice names you can do a reverse DNS-Lookup of the entries from the first request and display them to the user. There are some examples of how to do this in different languages: NodeJS (prefered), NodeJS, Python 2, Python 3, Java (Android), C#, Javascript(Browser)
 * Now you have a list of multiple names of servers which you can use to directly connect to with HTTP and preferably HTTPS.
 * Example: https://de1.api.radio-browser.info, https://de2.api.radio-browser.info, ..
 *
 * (Alternative: query the DNS SRV record of _api._tcp.radio-browser.info which gives you the list of server names directly without reverse dns lookups.)
 *
 * 2. Randomize the list
 * Either let the user choose one server from the list with a dialog or
 * just randomize the list and choose the first entry of the now random list. If a request fails just retry the request with the next entry in the list.
 *
 * 3. Remember the following things
 * (done +) Send a speaking http agent string (e.g. mycoolapp/1.4) -> UserAgentInterceptor
 * (done +) Send /json/url requests for every click the user makes, this helps to mark stations as popular and makes the database more usefull to other people.
 * -> sendGetRequestToMarkRadioStationAsPopular. Когда пользователь кликает по радиостанции, он попадает в HomeRadioFragment с аргументом, запрос отправляется оттуда
 * (ok +) Send feature requests/bugs to GitLab (раньше в документации был GitHub)
 *
 * 4. Continue with the docs of the server
 * I try to keep them all at the same version, so they should always all be the same.
 * Here are some examples of working servers:
 * https://de1.api.radio-browser.info (на сентябрь 2026 в документации остался только он - он же FALLBACK_SERVER в Constants)
 * Click the links to find out about the API. Please remember that any of them may go down in the future, which means that you always should follow the previous steps in your app.
 */
class NetworkRadioDataSource @Inject constructor(
    private val radioServiceWrapper: IRadioServiceWrapper,
    @ApplicationContext private val context: Context
) : INetworkRadioDataSource {
    companion object {
        private const val TAG = "NetworkRadioDataSource"
    }

    override suspend fun getCountryList(): List<CountryRemote> =
        requestFromAnyServer(isValidResult = { it.isNotEmpty() }) { getCountryList() } ?: listOf()

    // Список станций страны. Серверы перебираются по кругу, пока не пройдёт SERVER_SEARCH_TIME
    // (полоса загрузки PROGRESS_TIMEOUT рассчитана так, чтобы не пропасть раньше, чем закончится перебор)
    override suspend fun getRadioStationList(countryCode: String): Resource<List<RadioStationRemote>> {
        val radioStationRemoteList = requestFromAnyServer(
            searchTimeMs = SERVER_SEARCH_TIME,
            isValidResult = { it.isNotEmpty() }
        ) {
            getRadioStationList(searchTerm = countryCode.uppercase())
        } ?: return Resource.error(SERVER_IS_DOWN, listOf())

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
            Resource.error(SERVER_IS_DOWN, listOf())
        }
    }

    // Возвращает true, если ни один сервер не ответил
    override suspend fun sendGetRequestToMarkRadioStationAsPopular(stationUuid: String): Boolean =
        requestFromAnyServer(isValidResult = { it.ok != null }) {
            markStationAsPopular(stationUuid = stationUuid)
        } == null

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
     *
     * @return ответ первого сервера, для которого [isValidResult] вернул true, или null, если не ответил никто
     */
    private suspend fun <T> requestFromAnyServer(
        searchTimeMs: Long = 0L,
        isValidResult: (T) -> Boolean,
        request: suspend IRadioService.() -> T
    ): T? {
        // Шаг 1 документации API: получить список доступных серверов через DNS.
        // distinct() - у одного сервера несколько IP-адресов (IPv4 и IPv6), и DNS возвращает его имя столько раз,
        // сколько у него адресов. Без distinct() мы бы ходили на один и тот же сервер по два раза подряд.
        // Шаг 2 документации: перемешать список, чтобы все пользователи приложения не нагружали один и тот же сервер
        val servers = updateDNSList().distinct().shuffled()

        // elapsedRealtime() - время с момента загрузки телефона. В отличие от System.currentTimeMillis() оно не прыгнет,
        // если пользователь (или сеть) переведёт часы, поэтому для измерения длительности берут именно его
        val searchStartTime = SystemClock.elapsedRealtime()
        var attempt = 0

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
                val result = radioServiceWrapper.getRadioService(baseURL).request()
                // Ответ получен. Если он нас устраивает - выходим из цикла и из метода, остальные серверы не трогаем
                if (isValidResult(result)) return result
                Log.d(TAG, "Сервер $baseURL прислал пустой ответ")
            } catch (e: CancellationException) {
                // Корутину отменили (например, пользователь выбрал другой плейлист, и старая загрузка больше не нужна).
                // Отмену нельзя "проглатывать" вместе с остальными ошибками: если её поймать и продолжить цикл,
                // мы будем ходить по серверам ради результата, который уже никому не нужен. Поэтому пробрасываем дальше
                throw e
            } catch (e: Exception) {
                // Любая ошибка этого сервера - не повод сдаваться, просто пробуем следующий:
                // IOException (нет сети, таймаут), HttpException (например, 502 или 503),
                // JsonSyntaxException (сервер жив, но ответил не тем, что мы ждём)
                Log.d(TAG, "Сервер $baseURL: ${e.javaClass.simpleName}: ${e.message}")
            }

            // Телефон вообще не подключён к сети (режим полёта, выключены Wi-Fi и мобильные данные) - перебирать серверы
            // бессмысленно: каждый ответит той же ошибкой. Раньше пользователь ждал до конца SERVER_SEARCH_TIME
            if (!hasNetworkConnection()) {
                Log.d(TAG, "Нет подключения к сети - перебор серверов прекращаем")
                return null
            }

            // Пауза перед следующей попыткой - чтобы не завалить серверы запросами, если они отвечают ошибкой мгновенно.
            // Условие то же, что у цикла: паузу делаем, только если следующая попытка вообще будет.
            // delay() не занимает поток (в отличие от Thread.sleep) и умеет отменяться вместе с корутиной
            if (attempt < servers.size || searchTimeMs > 0) delay(SERVER_RETRY_DELAY)
        }

        // Никто не ответил. Что это значит для пользователя, решает вызывающий код:
        // для станций - Resource.error и диалог "сервер недоступен", для стран - пустой список
        return null
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