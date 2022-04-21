package com.myproject.radiojourney.domain.recommendedList

import com.myproject.radiojourney.model.local.CountryLocal
import com.myproject.radiojourney.model.presentation.RadioStationPresentation
import kotlinx.coroutines.flow.Flow

interface IRecommendedListInteractor {
    suspend fun getToken(): Int?
    suspend fun getRadioStationRecommendedList(): List<RadioStationPresentation>
}