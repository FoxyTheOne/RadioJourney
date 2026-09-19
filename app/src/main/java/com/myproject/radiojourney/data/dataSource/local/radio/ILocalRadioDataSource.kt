package com.myproject.radiojourney.data.dataSource.local.radio

import com.myproject.radiojourney.entities.local.CountryLocal
import com.myproject.radiojourney.entities.local.RadioStationLocal
import kotlinx.coroutines.flow.Flow

interface ILocalRadioDataSource {
    fun subscribeOnCountryList(): Flow<List<CountryLocal>>

    suspend fun getRadioStationSaved(radioStationUuid: String): RadioStationLocal?

    suspend fun setStationFavourite(radioStation: RadioStationLocal, isFavourite: Boolean)

    suspend fun saveCountryList(countryLocalList: List<CountryLocal>)

    suspend fun saveLastUsedRadioStationUrlAndCode(urlResolved: String, countryCode: String)

    suspend fun setIsHideInfoClicked(isHideInfoClicked: Boolean)

    suspend fun isHideInfoClicked(): Boolean
}