package com.myproject.radiojourney.domain.homeRadio

import com.myproject.radiojourney.model.presentation.CountryPresentation
import com.myproject.radiojourney.model.presentation.RadioStationPresentation
import kotlinx.coroutines.flow.Flow

interface IHomeRadioInteractor {
    fun subscribeOnCountryList(): Flow<List<CountryPresentation>>

    suspend fun isRadioStationStored(): Boolean
    suspend fun getRadioStationUrl(): String?
    suspend fun getRadioStationSaved(radioStationUrl: String): RadioStationPresentation?

    suspend fun saveRadioStationUrl(isStored: Boolean, radioStation: RadioStationPresentation)
    suspend fun saveFavouriteRadioStationUrl(isStored: Boolean, url: String)

    suspend fun getToken(): Int?
    suspend fun addStationToFavourites(
        userCreatorIdInt: Int,
        currentRadioStation: RadioStationPresentation
    )

    suspend fun isStationInFavourites(url: String): Boolean
    suspend fun deleteRadioStationFromFavourite(
        userCreatorIdInt: Int,
        currentRadioStation: RadioStationPresentation
    )
}