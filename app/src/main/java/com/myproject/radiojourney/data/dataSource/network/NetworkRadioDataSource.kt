package com.myproject.radiojourney.data.dataSource.network

import android.os.SystemClock
import android.util.Log
import com.myproject.radiojourney.data.dataSource.network.service.IRadioService
import com.myproject.radiojourney.data.dataSource.network.service.IRadioServiceWrapper
import com.myproject.radiojourney.entities.remote.CountryCodeRemote
import com.myproject.radiojourney.entities.remote.RadioStationRemote
import com.myproject.radiojourney.other.Constants.MAX_STATIONS_COUNT
import com.myproject.radiojourney.other.Constants.SERVER_IS_DOWN
import com.myproject.radiojourney.other.Constants.SERVER_SEARCH_TIME
import com.myproject.radiojourney.other.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
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
 * (ok +) Send feature requests/bugs to github
 *
 * 4. Continue with the docs of the server
 * I try to keep them all at the same version, so they should always all be the same.
 * Here are some examples of working servers:
 * https://de1.api.radio-browser.info, https://nl.api.radio-browser.info, https://at1.api.radio-browser.info
 * Click the links to find out about the API. Please remember that any of them may go down in the future, which means that you always should follow the previous steps in your app.
 */
class NetworkRadioDataSource @Inject constructor(
    private val radioServiceWrapper: IRadioServiceWrapper
) : INetworkRadioDataSource {
    companion object {
        private const val TAG = "NetworkRadioDataSource"
        private const val DNS_RETRY_DELAY = 1_000L
        private const val FALLBACK_SERVER = "de1.api.radio-browser.info"

        // Небольшая пауза перед попыткой на следующем сервере
        private const val SERVER_RETRY_DELAY = 1_000L
    }

    override suspend fun getCountryCodeList(): List<CountryCodeRemote> =
        requestFromAnyServer(isValidResult = { it.isNotEmpty() }) { getCountryCodeList() }
            ?: listOf()

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
     * Выполнить запрос на одном из серверов radio-browser.
     * Раньше этот перебор серверов был написан трижды (для стран, станций и отметки популярности), с разной обработкой ошибок.
     *
     * - Список серверов перемешивается (п. 2 документации API), каждый сервер пробуется хотя бы раз;
     * - если задан searchTimeMs, серверы перебираются по кругу, пока не пройдёт это время;
     * - ошибка одного сервера (нет соединения, ошибка HTTP, некорректный ответ) - переходим к следующему;
     * - отмена корутины (выбран другой плейлист) прекращает перебор.
     *
     * @return результат первого сервера, для которого isValidResult == true, или null
     */
    private suspend fun <T> requestFromAnyServer(
        searchTimeMs: Long = 0L,
        isValidResult: (T) -> Boolean,
        request: suspend IRadioService.() -> T
    ): T? {
        // 1. Get a list of available servers. Distinct - DNS возвращает одно и то же имя сервера для каждого его IP-адреса.
        // 2. Randomize the list and choose the first entry of the now random list. If a request fails just retry the request with the next entry in the list.
        val servers = updateDNSList().distinct().shuffled()
        val searchStartTime = SystemClock.elapsedRealtime()
        var attempt = 0

        while (servers.isNotEmpty() &&
            (attempt < servers.size || SystemClock.elapsedRealtime() - searchStartTime < searchTimeMs)
        ) {
            val baseURL = "https://${servers[attempt % servers.size]}"
            attempt++
            Log.d(TAG, "Запрос к серверу $baseURL, попытка $attempt")

            try {
                val result = radioServiceWrapper.getRadioService(baseURL).request()
                if (isValidResult(result)) return result
                Log.d(TAG, "Сервер $baseURL прислал пустой ответ")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // IOException (нет соединения, таймаут), HttpException (например, 502/503), JsonSyntaxException (не тот ответ)
                Log.d(TAG, "Сервер $baseURL: ${e.javaClass.simpleName}: ${e.message}")
            }

            // Пауза перед следующей попыткой, если будет следующая
            if (attempt < servers.size || searchTimeMs > 0) delay(SERVER_RETRY_DELAY)
        }
        return null
    }

    // do the DNS request
    // suspend + withContext(IO): поиск DNS блокирует поток, а пауза между попытками - delay (не занимает поток, как Thread.sleep)
    private suspend fun updateDNSList(attemptsLeft: Int = 3): List<String> =
        withContext(Dispatchers.IO) {
            val listDNSResult = mutableListOf<String>()
            try {
                // add all round robin servers one by one to select them separately
                val list = InetAddress.getAllByName("all.api.radio-browser.info")
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
            } else if (attemptsLeft > 1) {
                delay(DNS_RETRY_DELAY)
                updateDNSList(attemptsLeft - 1)
            } else {
                Log.d(TAG, "Список серверов не получен, используем $FALLBACK_SERVER")
                listOf(FALLBACK_SERVER)
            }
        }

    private fun throwHttpException() {
        Log.d(TAG, "Попытка вызвать ошибку The server is down")
        // Создаем объект HttpException с указанием кода ошибки HTTP
//            Код ошибки HTTP, который вы должны указать в statusCode, зависит от конкретной ошибки, которую вы хотите имитировать.
//
//            Некоторые наиболее распространенные коды ошибок HTTP:
//
//            - 400 Bad Request: ошибка запроса клиента (неверный синтаксис, неправильные параметры и т. д.)
//            - 401 Unauthorized: требуется аутентификация пользователя для доступа к ресурсу
//            - 403 Forbidden: доступ к ресурсу запрещен, у клиента нет прав доступа
//            - 404 Not Found: ресурс не найден
//            - 500 Internal Server Error: ошибка сервера, общая внутренняя ошибка
//            - Если вам нужно имитировать случай, когда сервер недоступен, вы можете использовать код ошибки HTTP 503 Service Unavailable. Этот код ошибки указывает, что сервер не может обработать запрос в данный момент из-за временной недоступности.
        val statusCode = 503

        val response = Response.error<Any>(statusCode, ResponseBody.create(null, "error message"))
        val httpException = HttpException(response)

        throw httpException
    }
}