package com.myproject.radiojourney.data.dataSource.network.service

import com.myproject.radiojourney.data.dataSource.network.entity.CountryCodeRemote
import com.myproject.radiojourney.data.dataSource.network.entity.RadioStationRemote
import com.myproject.radiojourney.data.dataSource.network.entity.StreamInfoResult
import com.myproject.radiojourney.other.Constants.MAX_STATIONS_COUNT
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit2
 * 1. Превращаем JSON в объекты и сохраняем в папку model -> remote
 * 2. Запрос нам нужно будет делать по динамической BASE_URL. Поэтому retrofit инициал. в hilt не на прямую, а с помощью "обёртки".
 *  Составляем end point нашего запроса, создаём для этого интерфейс (IRadioService)
 * 3. Создаём обертку, которую будем инициализировать с помощью hilt (класс RadioServiceWrapper и интерфейс).
 * В ней создаём retrofit сервис.
 * 4. Описываем метод в файле Module для hilt
 */
interface IRadioService {
    companion object {
        private const val BASE_PATH = "json"
        private const val COUNTRY_CODE_PATH = "countrycodes"
        private const val STATIONS_PATH = "stations"
        private const val BY_COUNTRY_CODE_EXACT_PATH = "bycountrycodeexact"
        private const val URL_PATH = "url"
    }

    // Пример - http://de1.api.radio-browser.info/json/countrycodes?hidebroken=true
    @GET("$BASE_PATH/{countrycodes}")
    suspend fun getCountryCodeList(
        @Path("countrycodes") countryCodes: String = COUNTRY_CODE_PATH,
        // Считать только работающие станции (как и в списке станций ниже)
        @Query("hidebroken") hideBroken: Boolean = true
    ): List<CountryCodeRemote>

    // Пример - http://de1.api.radio-browser.info/{format}/stations/bycountrycodeexact/{searchterm}
    // Пример - http://de1.api.radio-browser.info/json/stations/bycountrycodeexact/US?hidebroken=true&order=clickcount&reverse=true&limit=500
    @GET("$BASE_PATH/{stations}/{bycountrycodeexact}/{searchterm}")
    suspend fun getRadioStationList(
        @Path("stations") stations: String = STATIONS_PATH,
        @Path("bycountrycodeexact") byCountryCodeExact: String = BY_COUNTRY_CODE_EXACT_PATH,
        @Path("searchterm") searchTerm: String,
        // Не присылать станции, которые не прошли последнюю проверку сервера radio-browser (lastcheckok = 0)
        @Query("hidebroken") hideBroken: Boolean = true,
        // Сортировка и лимит - на сервере. Без них сервер отдаёт максимум 1000 станций в произвольном порядке
        // (у US их больше 8000), и "самые популярные" выбирались только из этой случайной 1000
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("limit") limit: Int = MAX_STATIONS_COUNT
    ): List<RadioStationRemote>

    //    Station click counter
//
//    Increase the click count of a station by one. This should be called everytime when a user starts playing a stream to mark the stream more popular than others. Every call to this endpoint from the same IP address and for the same station only gets counted once per day. The call will return detailed information about the stream, supported output formats: JSON, XML ,PLS ,M3U
//    Syntax:
//    http://de1.api.radio-browser.info/xml/url/stationuuid
//    http://de1.api.radio-browser.info/json/url/stationuuid
//    http://de1.api.radio-browser.info/pls/url/stationuuid
//    http://de1.api.radio-browser.info/m3u/url/stationuuid
    @GET("$BASE_PATH/{url}/{stationuuid}")
    suspend fun markStationAsPopular(
        @Path("url") url: String = URL_PATH,
        @Path("stationuuid") stationUuid: String
    ): StreamInfoResult
}