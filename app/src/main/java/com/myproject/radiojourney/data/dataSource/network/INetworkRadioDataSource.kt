package com.myproject.radiojourney.data.dataSource.network

import com.myproject.radiojourney.data.dataSource.network.entity.CountryRemote
import com.myproject.radiojourney.data.dataSource.network.entity.RadioStationRemote
import com.myproject.radiojourney.other.Resource

/**
 * Remote data source: запросы к API radio-browser.
 *
 * Возвращает модели remote (как их присылает сервер). Преобразование в модели приложения - в репозитории
 */
interface INetworkRadioDataSource {
    suspend fun getCountryList(): List<CountryRemote>

    //    suspend fun getRadioStationList(countryCode: String): List<RadioStationRemote>
    suspend fun getRadioStationList(countryCode: String): Resource<List<RadioStationRemote>>

    //    suspend fun getAllRadioStationsList(): List<RadioStationRemote>
    suspend fun sendGetRequestToMarkRadioStationAsPopular(stationUuid: String): Boolean
}