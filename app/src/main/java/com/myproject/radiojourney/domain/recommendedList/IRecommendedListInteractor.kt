package com.myproject.radiojourney.domain.recommendedList

import com.myproject.radiojourney.model.presentation.RadioStationPresentation

interface IRecommendedListInteractor {
    suspend fun getToken(): Int?
    suspend fun getRadioStationRecommendedList(): List<RadioStationPresentation>
}