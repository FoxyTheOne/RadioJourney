package com.myproject.radiojourney.domain.mainRadioUseCase

import com.myproject.radiojourney.domain.model.RadioStation

/**
 * UseCase нижнего плеера (MainViewModel): признак избранного для станций плейлиста,
 * запоминание последней станции и отметка "станцию слушают" на сервере
 */
interface IMainRadioUseCase {
    // Станции плейлиста с признаком избранного из Room
    suspend fun withFavouriteFlags(radioStations: List<RadioStation>): List<RadioStation>
    suspend fun saveLastUsedRadioStationUrlAndCode(urlResolved: String, countryCode: String)
    suspend fun markRadioStationAsPopularSendGetRequest(stationUuid: String): Boolean
}