package com.myproject.radiojourney.domain.favouriteListUseCase

import com.myproject.radiojourney.domain.model.RadioStation

/**
 * UseCase экрана "Избранное": список станций, отмеченных звездой
 */
interface IFavouriteListUseCase {
    suspend fun getRadioStationFavouriteList(): List<RadioStation>
}