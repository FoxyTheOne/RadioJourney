package com.myproject.radiojourney.domain.iRepository

import com.myproject.radiojourney.entities.local.CountryLocal
import com.myproject.radiojourney.entities.local.RadioStationLocal
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

    suspend fun getRadioStationList(countryCode: String): List<RadioStationLocal>

    suspend fun saveLastUsedRadioStationUrlAndCode(urlResolved: String, countryCode: String)
}