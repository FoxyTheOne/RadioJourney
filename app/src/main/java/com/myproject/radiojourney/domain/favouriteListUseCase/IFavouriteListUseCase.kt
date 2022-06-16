package com.myproject.radiojourney.domain.favouriteListUseCase

import com.myproject.radiojourney.entities.presentation.RadioStationPresentation

interface IFavouriteListUseCase {
    suspend fun getRadioStationFavouriteList(isStationInFavorite: Boolean): List<RadioStationPresentation>

    suspend fun addStationInRoomToFavourites(currentRadioStation: RadioStationPresentation)
    suspend fun deleteStationInRoomFromFavourite(currentRadioStation: RadioStationPresentation)
}