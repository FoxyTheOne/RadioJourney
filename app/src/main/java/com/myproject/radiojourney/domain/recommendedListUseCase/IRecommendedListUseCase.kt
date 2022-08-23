package com.myproject.radiojourney.domain.recommendedListUseCase

import com.myproject.radiojourney.entities.presentation.RadioStationPresentation

interface IRecommendedListUseCase {
    suspend fun getRadioStationRecommendedList(): List<RadioStationPresentation>

    suspend fun addStationInRoomToFavourites(currentRadioStation: RadioStationPresentation)
    suspend fun deleteStationInRoomFromFavourite(currentRadioStation: RadioStationPresentation)
}