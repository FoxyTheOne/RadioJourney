package com.myproject.radiojourney.domain.mainRadioUseCase

import com.myproject.radiojourney.domain.iRepository.IMainRadioStationRepository
import com.myproject.radiojourney.domain.model.RadioStation
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin. Работает только с Repository.
 *
 * Раньше этот use case получал MediaItem (класс media3) и возвращал RadioStationPresentation (модель экрана), то есть domain
 * зависел и от библиотеки плеера, и от presentation. Теперь MediaItem переводит в RadioStation плеерный слой (RadioStationMediaItems),
 * а в модель экрана - ViewModel
 */
class MainRadioUseCase @Inject constructor(
    private val mainRadioStationRepository: IMainRadioStationRepository
) : IMainRadioUseCase {

    // Станции плейлиста приходят из сервиса плеера без признака избранного: его знает только Room.
    // Если станция уже сохранена, узнаём её isFavourite, если нет - false
    override suspend fun withFavouriteFlags(radioStations: List<RadioStation>): List<RadioStation> =
        radioStations.map { radioStation ->
            val savedRadioStation = mainRadioStationRepository.getSavedRadioStation(radioStation.stationUuid)
            radioStation.copy(isFavourite = savedRadioStation?.isFavourite ?: false)
        }

    override suspend fun saveLastUsedRadioStationUrlAndCode(urlResolved: String, countryCode: String) =
        mainRadioStationRepository.saveLastUsedRadioStationUrlAndCode(urlResolved, countryCode)

    override suspend fun markRadioStationAsPopularSendGetRequest(stationUuid: String): Boolean =
        mainRadioStationRepository.markRadioStationAsPopularSendGetRequest(stationUuid)
}