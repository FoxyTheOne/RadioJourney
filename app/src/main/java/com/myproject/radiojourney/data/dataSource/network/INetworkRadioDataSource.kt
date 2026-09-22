package com.myproject.radiojourney.data.dataSource.network

import com.myproject.radiojourney.data.dataSource.network.entity.CountryRemote
import com.myproject.radiojourney.data.dataSource.network.entity.RadioStationRemote
import com.myproject.radiojourney.other.Constants.MAX_STATIONS_COUNT
import com.myproject.radiojourney.other.Resource

/**
 * Remote data source: запросы к API radio-browser.
 *
 * Возвращает модели remote (как их присылает сервер). Преобразование в модели приложения - в репозитории
 */
interface INetworkRadioDataSource {
    suspend fun getCountryList(): List<CountryRemote>

    // limit - сколько самых популярных станций страны запросить. Меньше MAX_STATIONS_COUNT - запасной короткий список
    // (см. POPULAR_STATIONS_FALLBACK_COUNT): его запрашивают сразу после неудачи с полным, поэтому серверы перебираются один раз
    suspend fun getRadioStationList(
        countryCode: String,
        limit: Int = MAX_STATIONS_COUNT
    ): Resource<List<RadioStationRemote>>

    suspend fun sendGetRequestToMarkRadioStationAsPopular(stationUuid: String): Boolean
}