package com.myproject.radiojourney.data.dataSource.network

import android.os.SystemClock
import android.util.Log
import com.myproject.radiojourney.data.dataSource.network.service.IRadioServiceWrapper
import com.myproject.radiojourney.entities.remote.CountryCodeRemote
import com.myproject.radiojourney.entities.remote.RadioStationRemote
import com.myproject.radiojourney.entities.remote.StreamInfoResult
import com.myproject.radiojourney.other.Constants.MAX_STATIONS_COUNT
import com.myproject.radiojourney.other.Constants.SERVER_SEARCH_TIME
import com.myproject.radiojourney.other.Constants.SERVER_IS_DOWN
import com.myproject.radiojourney.other.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Vector
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
 * Send a speaking http agent string (e.g. mycoolapp/1.4)
 * Send /json/url requests for every click the user makes, this helps to mark stations as popular and makes the database more usefull to other people.
 * Send feature requests/bugs to github
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

        //        <!-- 004 claude
        private const val DNS_RETRY_DELAY = 1_000L
        private const val FALLBACK_SERVER = "de1.api.radio-browser.info"
        // 004 claude -->
    }

    // API -> Для того, чтобы воспользоваться API радиостанций, нужно выполнить несколько шагов.
    // These steps should be done in your APP or program.
//    override suspend fun getCountryCodeList(): List<CountryCodeRemote> {
    override suspend fun getCountryCodeList(): List<CountryCodeRemote> {
        try {

            // 1. Get a list of available servers.
            // Do a DNS-lookup of 'all.api.radio-browser.info'. This gives you a list of all available servers.
            val listDNSResultArray = updateDNSList()

            // 2. Randomize the list and choose the first entry of the now random list. If a request fails just retry the request with the next entry in the list.
            listDNSResultArray.shuffle()

            // Пробуем перебирать сервера
            var countryCodeRemoteList =
                listOf<CountryCodeRemote>() // Пустой массив для результата запроса

            val resultDNSIterator = listDNSResultArray.iterator()
            while (resultDNSIterator.hasNext()) {
                val baseURL = "https://${resultDNSIterator.next()}"
                Log.d(TAG, "результат baseURL = $baseURL")

                try {
                    // radioServiceWrapper - обёртка. Инициализируем retrofit и получаем сервис:
                    val radioService = radioServiceWrapper.getRadioService(baseURL)
                    // И затем делаем запрос getCountryCodeList():
                    countryCodeRemoteList = radioService.getCountryCodeList()

                    if (countryCodeRemoteList != emptyList<String>()) break
                } catch (e: CancellationException) {
                    throw e // загрузку отменили - это не ошибка сервера, прекращаем перебор
                } catch (e: HttpException) {
                    // Сервер ответил ошибкой (например, 502) - пробуем следующий, а не прекращаем перебор
                    Log.d(
                        TAG,
                        "HttpException: ${e.code()}. Continue searching baseURL in resultDNSIterator"
                    )
                    continue
                } catch (e: SocketTimeoutException) {
                    Log.d(
                        TAG,
                        "Exception: ${e.message}. Failed to connect to baseURL. Continue searching baseURL in resultDNSIterator"
                    )
                    e.printStackTrace()
                    continue
                } catch (e: IOException) {
                    Log.d(
                        TAG,
                        "Exception: ${e.message}. Problem with the server. Continue searching baseURL in resultDNSIterator"
                    )
                    e.printStackTrace()
                    continue
                } catch (e: RuntimeException) {
                    // Например, JsonSyntaxException - сервер прислал не тот ответ. Раньше такое исключение роняло приложение
                    Log.d(
                        TAG,
                        "Unexpected server response: ${e.message}. Continue searching baseURL in resultDNSIterator"
                    )
                    continue
                }
            }

//            countryCodeRemoteList.forEach { result ->
//                Log.d(TAG, "результат1 запроса countryCodeRemoteList: $result")
//            }
            // Обратить внимание, что могут прилететь коды стран, написанные маленькими буквами. При получении результата и переводе в локальные данные, объединить

//            countryCodeRemoteList =
//                listOf<CountryCodeRemote>()
            return countryCodeRemoteList

        } catch (e: HttpException) {
            Log.d(TAG, "Exception: ${e.message}. The server is down")
            e.printStackTrace()
            return listOf()
        }
    }

    /**
     *  API.radio-browser.info docs - Remember the following things:
     *  (done +) Send a speaking http agent string (e.g. mycoolapp/1.4) -> I made a class UserAgentInterceptor and added it in RadioServiceWrapper
     *  (done +) Send /json/url requests for every click the user makes, this helps to mark stations as popular and makes the database more useful to other people. -> Метод sendGetRequestToMarkRadioStationAsPopular. Когда пользователь кликает по радиостанции, от попадает в HomeRadioFragment с аргументом, поэтом попробую начать передавать информацию оттуда
     *  (ok +) Send feature requests/bugs to github
     */

    // API -> Для того, чтобы воспользоваться API радиостанций, нужно выполнить несколько шагов.
    // These steps should be done in your APP or program.
    override suspend fun getRadioStationList(countryCode: String): Resource<List<RadioStationRemote>> {
//    override suspend fun getRadioStationList(countryCode: String): List<RadioStationRemote> {
        try {

            // 1. Get a list of available servers.
            // Do a DNS-lookup of 'all.api.radio-browser.info'. This gives you a list of all available servers.
            val listDNSResultArray = updateDNSList()

            // 2. Randomize the list and choose the first entry of the now random list. If a request fails just retry the request with the next entry in the list.
            listDNSResultArray.shuffle()

            // Пробуем перебирать сервера
            var radioStationRemoteList =
                listOf<RadioStationRemote>() // Пустой массив для результата запроса
//            var radioStationLowerCaseList =
//                listOf<RadioStationRemote>()
//            var radioStationUpperCaseList =
//                listOf<RadioStationRemote>()

            // <!-- 003-6 claude
//            val resultDNSIterator = listDNSResultArray.iterator()

            // Перебираем серверы по кругу, пока один из них не ответит. distinct() - DNS возвращает одно и то же имя сервера для каждого его IP-адреса.
            // Первый круг проходим всегда целиком (каждый сервер пробуем хотя бы раз), следующие круги - пока не прошло SERVER_SEARCH_TIME.
            // Полоса загрузки (PROGRESS_TIMEOUT) рассчитана так, чтобы не пропасть раньше, чем закончится перебор
            val servers = listDNSResultArray.distinct()
            val searchStartTime = SystemClock.elapsedRealtime()
            var attempt = 0
            // 003 claude -->

//            while (resultDNSIterator.hasNext()) {
//                val baseURL = "https://${resultDNSIterator.next()}"
//                Log.d(TAG, "результат baseURL = $baseURL")

            while (servers.isNotEmpty() &&
                (attempt < servers.size || SystemClock.elapsedRealtime() - searchStartTime < SERVER_SEARCH_TIME)
            ) {
                val baseURL = "https://${servers[attempt % servers.size]}"
                attempt++
                Log.d(TAG, "результат baseURL = $baseURL, попытка $attempt")

                // 003 claude -->

                try {
                    // radioServiceWrapper - обёртка. Инициализируем retrofit и получаем сервис:
                    val radioService = radioServiceWrapper.getRadioService(baseURL)

//                    // И затем делаем запрос getCountryCodeList():
//                    radioStationLowerCaseList =
//                        radioService.getRadioStationList(searchTerm = countryCode.lowercase())
//                    radioStationUpperCaseList =
//                        radioService.getRadioStationList(searchTerm = countryCode.uppercase())
//                    // Нет, двойной запрос делать не нужно! В данном случае, видимо, это предусмотрено. По обоим запросам всегда прилетает одинаковое количество станций

                    radioStationRemoteList =
                        radioService.getRadioStationList(searchTerm = countryCode.uppercase())

                    if (radioStationRemoteList != emptyList<RadioStationRemote>()) {
                        Log.d(
                            TAG,
                            "Успешный запрос. Получен результат radioStationRemoteList.size = ${radioStationRemoteList.size}"
                        )
                        break
                    }

                    // <!-- 005 claude
                    // Сервер ответил, но прислал пустой список. Раньше следующая попытка в этом случае шла сразу, без паузы,
                    // и все попытки заканчивались за доли секунды
                    Log.d(
                        TAG,
                        "Попытка связаться с сервером: получен пустой список. Continue searching baseURL in resultDNSIterator"
                    )
                    delay(1_000L) // небольшая пауза перед следующей попыткой
                    // 005 claude -->

                } catch (e: CancellationException) {
                    throw e // загрузку отменили (выбран другой плейлист) - прекращаем перебор
                } catch (e: SocketTimeoutException) {
                    Log.d(
                        TAG,
                        "Попытка связаться с сервером. Exception: ${e.message}. Failed to connect to baseURL. Continue searching baseURL in resultDNSIterator"
                    )
                    e.printStackTrace()
                    delay(1_000L) // небольшая пауза перед следующей попыткой // 003 claude
                    continue
                } catch (e: IOException) {
                    Log.d(
                        TAG,
                        "Попытка связаться с сервером. Exception: ${e.message}. Problem with the server. Continue searching baseURL in resultDNSIterator"
                    )
                    e.printStackTrace()
                    delay(1_000L) // небольшая пауза перед следующей попыткой // 003 claude
                    continue

                    // <!-- 004 claude
                } catch (e: HttpException) {
                    // Сервер ответил ошибкой (например, 502/503). Раньше это исключение ловилось только снаружи цикла:
                    // повторных попыток не было, и сразу (без ожидания) открывался пустой список
                    Log.d(
                        TAG,
                        "Попытка связаться с сервером. HttpException: ${e.code()} ${e.message()}. Continue searching baseURL in resultDNSIterator"
                    )
                    delay(1_000L) // небольшая пауза перед следующей попыткой
                    continue
                    // 004 claude -->

                } catch (e: RuntimeException) {
                    // Например, JsonSyntaxException - сервер прислал не тот ответ. Раньше такое исключение роняло приложение
                    Log.d(
                        TAG,
                        "Unexpected server response: ${e.message}. Continue searching baseURL in resultDNSIterator"
                    )
                    delay(1_000L)
                    continue
                }
            }

//            if (radioStationLowerCaseList != emptyList<RadioStationRemote>()) {
//                radioStationLowerCaseList.forEach {
//                    it.countrycode.uppercase()
//                }
//            }
//            Log.d(TAG, "Произведено преобразование uppercase()")
//
//            radioStationRemoteList = radioStationLowerCaseList + radioStationUpperCaseList
//            Log.d(TAG, "Списки объединены в radioStationRemoteList.size = ${radioStationRemoteList.size}")

//            throwHttpException() // for testing

            // !!! ПРОБЛЕМА:
            // Иногда получаем слишком длинный список радиостанций, из-за чего программа зависает.
            // РЕШЕНИЕ:
            // 1. Пагинация - не получилась, переходим к следующему варианту решения.
            // 2. Уменьшить количество станций:
            // - Упорядочить список по количеству прослушиваний.
            // - Затем оставить 1000 самых популярных.
            // - Упорядочить по алфавиту и затем передать как результат выполнение метода

            // ! Операции работают с пустым списком корректно
            // compareBy<RadioStationRemote> сортирует по возрастанию, а вам нужно по убыванию популярности
            // Для больших списков лучше использовать последовательную обработку:
            try {
                val resultStationsRemote = radioStationRemoteList
                    .asSequence()
                    // Фильтрация невалидных станций
                    .filter { station ->

                        // <!-- 004 claude
//                        station.name.isNotBlank() && station.url.isNotBlank()

                        station.name.isNotBlank() && station.url.isNotBlank() &&
                                station.lastcheckok == 1 // на случай, если сервер не учёл hidebroken
                        // 004 claude -->

                    }
                    .sortedByDescending { it.clickcount }
                    .take(MAX_STATIONS_COUNT)
                    .sortedBy { station ->
                        station.name.trim().lowercase()
                    }
                    .toList()

                // Логирование для отладки
                if (resultStationsRemote.size >= 3) {
                    Log.d(
                        TAG,
                        "TOP by clicks: ${resultStationsRemote[0].clickcount}, ${resultStationsRemote[1].clickcount}, ${resultStationsRemote[2].clickcount}"
                    )

                    Log.d(
                        TAG,
                        "TOP by name: ${resultStationsRemote[0].name}, ${resultStationsRemote[1].name}, ${resultStationsRemote[2].name}"
                    )
                } else if (resultStationsRemote.isNotEmpty()) {
                    Log.d(TAG, "Processed ${resultStationsRemote.size} stations")
                } else {
                    Log.w(TAG, "No valid stations after filtering!")
                }

                return Resource.success(resultStationsRemote)

        } catch (e: RuntimeException) {
            // Например, у станции нет названия (null пришёл в не-null поле). Раньше здесь создавался Resource.error,
            // который никуда не возвращался, и отдавался необработанный список. Теперь сообщаем об ошибке
            Log.d(TAG, "Ошибка обработки списка станций: ${e.message}")
            return Resource.error(SERVER_IS_DOWN, listOf())
        }

        } catch (e: HttpException) {
            Log.d(TAG, "Попытка связаться с сервером. Exception: ${e.message}. The server is down")
            e.printStackTrace()
            return Resource.error(SERVER_IS_DOWN, listOf())
        }
    }

    // API -> Для того, чтобы воспользоваться API радиостанций, нужно выполнить несколько шагов.
    // These steps should be done in your APP or program.
    override suspend fun sendGetRequestToMarkRadioStationAsPopular(stationUuid: String): Boolean {
        var isServerDown = false

        try {
            // 1. Get a list of available servers.
            // Do a DNS-lookup of 'all.api.radio-browser.info'. This gives you a list of all available servers.
            val listDNSResultArray = updateDNSList()

            // 2. Randomize the list and choose the first entry of the now random list. If a request fails just retry the request with the next entry in the list.
            listDNSResultArray.shuffle()

            // Пробуем перебирать сервера
            var streamInfoResult: StreamInfoResult // Сюда запишем ответ с сервера

            val resultDNSIterator = listDNSResultArray.iterator()

            while (resultDNSIterator.hasNext()) {
                val baseURL = "https://${resultDNSIterator.next()}"
                Log.d(TAG, "результат baseURL = $baseURL")

                try {
                    // radioServiceWrapper - обёртка. Инициализируем retrofit и получаем сервис:
                    val radioService = radioServiceWrapper.getRadioService(baseURL)

                    streamInfoResult =
                        radioService.markStationAsPopular(stationUuid = stationUuid)

                    if (streamInfoResult.ok != null) {
                        Log.d(
                            TAG,
                            "Успешный запрос. Получен результат streamInfoResult = $streamInfoResult"
                        )
                        break
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: SocketTimeoutException) {
                    Log.d(
                        TAG,
                        "Exception: ${e.message}. Failed to connect to baseURL. Continue searching baseURL in resultDNSIterator"
                    )
                    e.printStackTrace()
                    continue
                } catch (e: IOException) {
                    Log.d(
                        TAG,
                        "Exception: ${e.message}. Problem with the server. Continue searching baseURL in resultDNSIterator"
                    )
                    e.printStackTrace()
                    continue
                } catch (e: RuntimeException) {
                    // Например, JsonSyntaxException - сервер прислал не тот ответ. Раньше такое исключение роняло приложение
                    Log.d(TAG, "Unexpected server response: ${e.message}. Continue searching baseURL in resultDNSIterator")
                    continue
                }
            }

        } catch (e: HttpException) {
            Log.d(TAG, "Exception: ${e.message}. The server is down")
            e.printStackTrace()
            isServerDown = true
        }

        return isServerDown
    }

    // do the DNS request
    // <!-- 003 claude
//    private fun updateDNSList(): MutableList<String> {

    // suspend + withContext(IO): поиск DNS блокирует поток, а пауза между попытками - delay (не занимает поток, как Thread.sleep)
    private suspend fun updateDNSList(attemptsLeft: Int = 3): MutableList<String> = withContext(Dispatchers.IO) {
    // 003 claude -->

        val listDNSResult = Vector<String>()
        try {
            // add all round robin servers one by one to select them separately
            val list = InetAddress.getAllByName("all.api.radio-browser.info")
            for (item in list) {

                // <!-- 004 claude
//                listDNSResult.add(item.canonicalHostName)

                // canonicalHostName делает обратный DNS-запрос. Если он не удался, вместо имени сервера возвращается IP-адрес,
                // а https-запрос по IP не пройдёт (сертификат выдан на имя) - такие результаты пропускаем
                val hostName = item.canonicalHostName
                if (hostName != item.hostAddress) listDNSResult.add(hostName)
                // 004 claude -->
            }
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        }
        val listDNSResultArray = listDNSResult.toTypedArray().toMutableList()

        listDNSResultArray.forEach { result ->
            Log.d(TAG, "результат listDNSResultArray: $result")
        }
//        listDNSResultArray.clear()
//        listDNSResultArray.add("https://de1.api.radio-broser.info/")
//        listDNSResultArray.add("https://de1.api.radio-brower.info/")
//        listDNSResultArray.add("https://de1.api.radio-browsr.info/")

        // <!-- 003 claude
//        return if (listDNSResultArray != emptyList<String>()) {
//            listDNSResultArray
//        } else {
//            updateDNSList()
//        }

        // Раньше здесь была бесконечная рекурсия: без интернета DNS не отвечает, updateDNSList() вызывал сам себя,
        // пока приложение не падало со StackOverflowError. Теперь несколько попыток, а потом пустой список -
        // вызывающий код переберёт 0 серверов и вернёт пустой результат (он обрабатывается как ошибка загрузки)
        if (listDNSResultArray.isNotEmpty()) {
            listDNSResultArray
        } else if (attemptsLeft > 1) {
            delay(DNS_RETRY_DELAY)
            updateDNSList(attemptsLeft - 1)
        } else {
            // Имена серверов так и не получили - пробуем сервер, известный из документации radio-browser
            Log.d(TAG, "Список серверов не получен, используем $FALLBACK_SERVER")
            mutableListOf(FALLBACK_SERVER)
        }
        // 003 claude -->

    }

    private fun <T> merge(first: List<T>, second: List<T>): List<T> {
        val list: MutableList<T> = ArrayList(first)
        list.addAll(second)
        return list
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