package com.myproject.radiojourney.data.dataSource.network

import com.myproject.radiojourney.entities.remote.CountryCodeRemote
import android.util.Log
import com.myproject.radiojourney.data.dataSource.network.service.IRadioServiceWrapper
import com.myproject.radiojourney.entities.remote.RadioStationRemote
import com.myproject.radiojourney.entities.remote.StreamInfoResult
import retrofit2.HttpException
import java.io.IOException
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*
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
    }

    // API -> Для того, чтобы воспользоваться API радиостанций, нужно выполнить несколько шагов.
    // These steps should be done in your APP or program.
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
                }
            }

//            countryCodeRemoteList.forEach { result ->
//                Log.d(TAG, "результат1 запроса countryCodeRemoteList: $result")
//            }
            // Обратить внимание, что могут прилететь коды стран, написанные маленькими буквами. При получении результата и переводе в локальные данные, объединить

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
    override suspend fun getRadioStationList(countryCode: String): List<RadioStationRemote> {
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

            val resultDNSIterator = listDNSResultArray.iterator()

            while (resultDNSIterator.hasNext()) {
                val baseURL = "https://${resultDNSIterator.next()}"
                Log.d(TAG, "результат baseURL = $baseURL")

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

            return radioStationRemoteList

        } catch (e: HttpException) {
            Log.d(TAG, "Exception: ${e.message}. The server is down")
            e.printStackTrace()
            return listOf()
        }
    }

    // API -> Для того, чтобы воспользоваться API радиостанций, нужно выполнить несколько шагов.
    // These steps should be done in your APP or program.
    override suspend fun sendGetRequestToMarkRadioStationAsPopular(stationUuid: String) {
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
                }
            }

        } catch (e: HttpException) {
            Log.d(TAG, "Exception: ${e.message}. The server is down")
            e.printStackTrace()
        }

    }

    // do the DNS request
    private fun updateDNSList(): MutableList<String> {

        val listDNSResult = Vector<String>()
        try {
            // add all round robin servers one by one to select them separately
            val list = InetAddress.getAllByName("all.api.radio-browser.info")
            for (item in list) {
                listDNSResult.add(item.canonicalHostName)
            }
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        }
        val listDNSResultArray = listDNSResult.toTypedArray().toMutableList()

        listDNSResultArray.forEach { result ->
            Log.d(TAG, "результат listDNSResultArray: $result")
        }

        return if (listDNSResultArray != emptyList<String>()) {
            listDNSResultArray
        } else {
            updateDNSList()
        }

    }

    private fun <T> merge(first: List<T>, second: List<T>): List<T> {
        val list: MutableList<T> = ArrayList(first)
        list.addAll(second)
        return list
    }
}