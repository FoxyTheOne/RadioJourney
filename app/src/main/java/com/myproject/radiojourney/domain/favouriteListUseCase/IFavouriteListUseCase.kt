package com.myproject.radiojourney.domain.favouriteListUseCase

import com.myproject.radiojourney.entities.presentation.RadioStationPresentation

interface IFavouriteListUseCase {
    suspend fun getRadioStationFavouriteList(): List<RadioStationPresentation>
}