package com.myproject.radiojourney.domain.favouriteList

import com.myproject.radiojourney.model.presentation.RadioStationPresentation

interface IFavouriteListUseCase {
    suspend fun getRadioStationFavouriteList(isStationInFavorite: Boolean): List<RadioStationPresentation>

    suspend fun addStationInRoomToFavourites(currentRadioStation: RadioStationPresentation)
    suspend fun deleteStationInRoomFromFavourite(currentRadioStation: RadioStationPresentation)
}