package com.myproject.radiojourney.data.dataSource.network

import com.myproject.radiojourney.entities.remote.CountryCodeRemote
import android.util.Log
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.myproject.radiojourney.data.dataSource.network.service.IRadioServiceWrapper
import com.myproject.radiojourney.entities.remote.RadioStationRemote
import com.myproject.radiojourney.utils.exoplayer.MusicService
import retrofit2.HttpException
import java.io.IOException
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException

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
                try {
                    val baseURL = "https://${resultDNSIterator.next()}"
                    Log.d(TAG, "результат baseURL = $baseURL")

                    // radioServiceWrapper - обёртка. Инициализируем retrofit и получаем сервис:
                    val radioService = radioServiceWrapper.getRadioService(baseURL)
                    // И затем делаем запрос getCountryCodeList():
                    countryCodeRemoteList = radioService.getCountryCodeList()

                    countryCodeRemoteList.forEach { result ->
                        Log.d(TAG, "результат запроса countryCodeRemoteList: $result")
                    }

                    if (countryCodeRemoteList != emptyList<String>()) break
                } catch (e: IOException) {
                    Log.d(TAG, "Exception: ${e.message}. Problem with the server")
                    e.printStackTrace()
                    continue
                }
            }

            return countryCodeRemoteList
        } catch (e: HttpException) {
            Log.d(TAG, "Exception: ${e.message}. The server is down")
            e.printStackTrace()
            return listOf()
        }
    }

    /**
     * TODO
     *  Remember the following things:
     *  Send a speaking http agent string (e.g. mycoolapp/1.4)
     *  Send /json/url requests for every click the user makes, this helps to mark stations as popular and makes the database more useful to other people.
     *  Send feature requests/bugs to github
     */

    // API -> Для того, чтобы воспользоваться API радиостанций, нужно выполнить несколько шагов.
    // These steps should be done in your APP or program.
    override suspend fun getRadioStationList(countryCode: String): List<RadioStationRemote> {
        // 1. Get a list of available servers.
        // Do a DNS-lookup of 'all.api.radio-browser.info'. This gives you a list of all available servers.
        val listDNSResultArray = updateDNSList()

        // 2. Randomize the list and choose the first entry of the now random list. If a request fails just retry the request with the next entry in the list.
        listDNSResultArray.shuffle()

        // Пробуем перебирать сервера
        var radioStationRemoteList =
            listOf<RadioStationRemote>() // Пустой массив для результата запроса

        val resultDNSIterator = listDNSResultArray.iterator()
        while (resultDNSIterator.hasNext()) {
            val baseURL = "https://${resultDNSIterator.next()}"
            Log.d(TAG, "результат baseURL = $baseURL")


            try {
                // radioServiceWrapper - обёртка. Инициализируем retrofit и получаем сервис:
                val radioService = radioServiceWrapper.getRadioService(baseURL)
                // И затем делаем запрос getCountryCodeList():
                radioStationRemoteList = radioService.getRadioStationList(searchTerm = countryCode)
            } catch (e5: SSLHandshakeException) {
                Log.d(TAG, "e5 - Ищем HttpDataSource HttpDataSourceException: Unable to connect, Caused by: java.net.SocketTimeoutException: SSL handshake timed out")
                e5.printStackTrace()
            } catch (e:SocketTimeoutException) {
                // TODO Ищем HttpDataSource HttpDataSourceException: Unable to connect, Caused by: java.net.SocketTimeoutException: SSL handshake timed out
                Log.d(TAG, "e - Ищем HttpDataSource HttpDataSourceException: Unable to connect, Caused by: java.net.SocketTimeoutException: SSL handshake timed out")
                e.printStackTrace()
            } catch (e1: HttpDataSource.HttpDataSourceException) {
                Log.d(TAG, "e1 - Ищем HttpDataSource HttpDataSourceException: Unable to connect, Caused by: java.net.SocketTimeoutException: SSL handshake timed out")
                e1.printStackTrace()
            } catch (e3: ExoPlaybackException) {
                Log.d(TAG,"e3 - Ищем HttpDataSource HttpDataSourceException: Unable to connect, Caused by: java.net.SocketTimeoutException: SSL handshake timed out")
                e3.printStackTrace()
            } catch (e2: IOException) {
                Log.d(TAG, "e2 - Ищем HttpDataSource HttpDataSourceException: Unable to connect, Caused by: java.net.SocketTimeoutException: SSL handshake timed out")
                e2.printStackTrace()
            } catch (e4: InternalError) {
                Log.d(TAG,"e4 - Ищем HttpDataSource HttpDataSourceException: Unable to connect, Caused by: java.net.SocketTimeoutException: SSL handshake timed out")
                e4.printStackTrace()
            }


            if (radioStationRemoteList != emptyList<String>()) {

//                // Исправим http на https
//                radioStationRemoteList.forEach {
//                    val urlResolved = it.url_resolved
////                    Log.d(TAG, "url_resolved станции: $urlResolved")
//
//                    val indexOfHttp = urlResolved.indexOf("http:")
////                    Log.d(TAG, "Индекс искомого сочетания http: $indexOfHttp")
//
//                    if (indexOfHttp != -1) {
//                        Log.d(TAG, "Индекс не -1, заменяем http: на https:")
//
//                        val newUrlResolved = urlResolved.replace("http:","https:")
//                        it.url_resolved = newUrlResolved
////                        println(it)
//
//                        Log.d(TAG, "Новый url_resolved станции: $newUrlResolved, записался как: ${it.url_resolved}")
//                    }
//                }

                Log.d(
                    TAG,
                    "Успешный запрос. Получен результат radioStationRemoteList $radioStationRemoteList, элемент[0]: ${radioStationRemoteList[0]}"
                )

                break
            }
        }

        return radioStationRemoteList
    }

//    // API -> Для того, чтобы воспользоваться API радиостанций, нужно выполнить несколько шагов.
//    // These steps should be done in your APP or program.
//    override suspend fun getAllRadioStationsList(): Map<String, RadioStationRemote> {
//        // 1. Get a list of available servers.
//        // Do a DNS-lookup of 'all.api.radio-browser.info'. This gives you a list of all available servers.
//        val listDNSResultArray = updateDNSList()
//
//        // 2. Randomize the list and choose the first entry of the now random list. If a request fails just retry the request with the next entry in the list.
//        listDNSResultArray.shuffle()
//
//        // Пробуем перебирать сервера.
//
//        // !!! Нам нужно получить полный список всех радиостанций для нашего сервиса.
//        // TODO после видеокурса переделать полный список в MAP, чтобы мы имели список из ключей-стран и значений - списка радиостанций. Предыдущие методы нам понадобятся так же, потому что список радиостанций лучше каждый раз обновлять - вдруг что-то обновилось на сервере
//        // Или же получать здесь список радиостанций по тому countrycode, который сейчас сохранён в shared preference??? Будет проще при добавлении в плейлист, возможно. А так же не нужно будет слишком много всего загрузать. Но будет ли такой код работать, для этого нужно, чтобы метод, который вызывает этот метод, срабатывал каждый раз при выборе новой радиостанции
//
//        // Для начала, находим список всех стран (countrycode)
//        var countryCodeRemoteList = getCountryCodeList()
//
//        // Затем запишем в список все радиостанции по всем значениям полученного списка стран
//        var allRadioStationRemoteList =
//            mutableListOf<RadioStationRemote>() // Пустой массив для результата запроса
//
//        val resultDNSIterator = listDNSResultArray.iterator()
//        while (resultDNSIterator.hasNext()) {
//            val baseURL = "https://${resultDNSIterator.next()}"
//            Log.d(TAG, "результат baseURL = $baseURL")
//
//            // radioServiceWrapper - обёртка. Инициализируем retrofit и получаем сервис:
//            val radioService = radioServiceWrapper.getRadioService(baseURL)
//            // И затем делаем запрос getCountryCodeList():
//            countryCodeRemoteList.forEach {
//                val countryRadioStationRemoteList = radioService.getRadioStationList(searchTerm = it.name)
//                allRadioStationRemoteList.addAll(countryRadioStationRemoteList)
//            }
//
//            Log.d(
//                TAG,
//                "Успешный запрос. Получен результат radioStationRemoteList $allRadioStationRemoteList, элемент[0]: ${allRadioStationRemoteList[0]}"
//            )
//
//            if (allRadioStationRemoteList != emptyList<String>()) break
//        }
//
//        return allRadioStationRemoteList
//    }

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
}