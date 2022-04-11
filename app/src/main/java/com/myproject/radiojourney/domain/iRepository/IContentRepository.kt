package com.myproject.radiojourney.domain.iRepository

import com.myproject.radiojourney.model.local.CountryLocal
import com.myproject.radiojourney.model.local.RadioStationFavouriteLocal
import com.myproject.radiojourney.model.local.RadioStationLocal
import com.myproject.radiojourney.model.local.UserWithStations
import com.myproject.radiojourney.model.presentation.RadioStationPresentation
import kotlinx.coroutines.flow.Flow

/**
 * Repository. Domain layer.
 */
interface IContentRepository {
    fun subscribeOnCountryList(): Flow<List<CountryLocal>>

    suspend fun getRadioStationList(countryCode: String): List<RadioStationLocal>

    suspend fun isRadioStationStored(): Boolean
    suspend fun getRadioStationUrl(): String?
    suspend fun getRadioStationSaved(radioStationUrl: String): RadioStationLocal?

    suspend fun saveRadioStationUrl(isStored: Boolean, radioStation: RadioStationPresentation)
    suspend fun saveFavouriteRadioStationUrl(isStored: Boolean, url: String)

    suspend fun getToken(): Int?
    suspend fun addStationToFavourites(currentRadioStationFavouriteLocal: RadioStationFavouriteLocal)

    suspend fun isStationInFavourites(url: String): Boolean
    suspend fun deleteRadioStationFromFavourite(currentRadioStationFavouriteLocal: RadioStationFavouriteLocal)

    suspend fun getUsersWithStations(): List<UserWithStations>
}