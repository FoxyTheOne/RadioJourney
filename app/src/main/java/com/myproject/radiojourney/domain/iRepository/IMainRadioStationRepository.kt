package com.myproject.radiojourney.domain.iRepository

import com.myproject.radiojourney.entities.local.CountryLocal
import com.myproject.radiojourney.entities.local.RadioStationLocal
import com.myproject.radiojourney.other.Resource
import kotlinx.coroutines.flow.Flow

interface IMainRadioStationRepository {
    fun subscribeOnCountryList(): Flow<List<CountryLocal>>

    suspend fun isRadioStationStored(): Boolean
    suspend fun getRadioStationUrl(): String?
    suspend fun getRadioStationSaved(radioStationUrlResolved: String): RadioStationLocal?

    // Поменять в Shared Preference setIsRadioStationStored на true. Сохранить в Shared Preference (url)
    suspend fun saveRadioStationUrl(isStored: Boolean, urlResolved: String)

    // И сохранить радиостанцию в Room
    suspend fun saveRadioStationInRoom(radioStationLocal: RadioStationLocal)

    // Добавить в избранное или убрать из него: меняется только флаг избранного
    suspend fun setStationFavourite(radioStationLocal: RadioStationLocal, isFavourite: Boolean)

    //    suspend fun getRadioStationList(countryCode: String): List<RadioStationLocal>
    suspend fun getRadioStationList(countryCode: String): Resource<List<RadioStationLocal>>

    suspend fun saveLastUsedRadioStationUrlAndCode(urlResolved: String, countryCode: String)
    suspend fun markRadioStationAsPopularSendGetRequest(stationUuid: String): Boolean

    suspend fun setIsHideInfoClicked(isHideInfoClicked: Boolean)
    suspend fun isHideInfoClicked(): Boolean
}