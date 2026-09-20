package com.myproject.radiojourney.domain.favouriteListUseCase

import com.myproject.radiojourney.domain.model.RadioStation

interface IFavouriteListUseCase {
    suspend fun getRadioStationFavouriteList(): List<RadioStation>
}