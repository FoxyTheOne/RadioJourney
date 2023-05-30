package com.myproject.radiojourney.domain.homeRadioUseCase

import com.myproject.radiojourney.entities.presentation.CountryPresentation
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import kotlinx.coroutines.flow.Flow

interface IHomeRadioUseCase {
    fun subscribeOnCountryList(): Flow<List<CountryPresentation>>

    suspend fun isRadioStationStored(): Boolean
    suspend fun getRadioStationUrl(): String?
    suspend fun getRadioStationSaved(radioStationUrlResolved: String): RadioStationPresentation?

    // Поменять в Shared Preference setIsRadioStationStored на true. Сохранить в Shared Preference (url)
    suspend fun saveRadioStationUrl(isStored: Boolean, urlResolved: String)

    // И сохранить радиостанцию в Room
    suspend fun saveRadioStationInRoom(radioStation: RadioStationPresentation)

    suspend fun addStationInRoomToFavourites(currentRadioStation: RadioStationPresentation)
    suspend fun deleteStationInRoomFromFavourite(currentRadioStation: RadioStationPresentation)

    suspend fun setRecommendedRadioStations(recommendedList: Map<String, String>)
}