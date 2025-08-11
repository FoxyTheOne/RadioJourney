package com.myproject.radiojourney.data.dataSource.local.radio

import com.myproject.radiojourney.entities.local.CountryLocal
import com.myproject.radiojourney.entities.local.RadioStationLocal
import kotlinx.coroutines.flow.Flow

interface ILocalRadioDataSource {
    fun subscribeOnCountryList(): Flow<List<CountryLocal>>

    suspend fun isRadioStationStored(): Boolean
    suspend fun getRadioStationUrl(): String?
    suspend fun getRadioStationSaved(radioStationUrl: String): RadioStationLocal?

    // Сохранить радиостанцию в Room
    suspend fun saveRadioStationInRoom(radioStation: RadioStationLocal)

    suspend fun saveCountryList(countryLocalList: MutableList<CountryLocal>)

    suspend fun saveLastUsedRadioStationUrlAndCode(urlResolved: String, countryCode: String)

    suspend fun setIsHideInfoClicked(isHideInfoClicked: Boolean)

    suspend fun isHideInfoClicked(): Boolean
}