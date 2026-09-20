package com.myproject.radiojourney.domain.favouriteListUseCase

import com.myproject.radiojourney.domain.iRepository.IFavoriteStationRepository
import com.myproject.radiojourney.domain.model.RadioStation
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin. Работает только с Repository.
 * Добавление в избранное и удаление из него - в общем ChangeFavouriteUseCase
 */
class FavouriteListUseCase @Inject constructor(
    private val favoriteStationRepository: IFavoriteStationRepository
) : IFavouriteListUseCase {

    override suspend fun getRadioStationFavouriteList(): List<RadioStation> =
        favoriteStationRepository.getFavoriteRadioStationList()
}