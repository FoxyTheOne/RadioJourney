package com.myproject.radiojourney.domain.favouriteListUseCase

import com.myproject.radiojourney.domain.iRepository.IFavoriteStationRepository
import com.myproject.radiojourney.domain.model.RadioStation
import javax.inject.Inject

/**
 * Domain layer, UseCase экрана "Избранное": отдаёт список станций со звездой.
 * Сама звезда (добавить или убрать) - в общем ChangeFavouriteUseCase: её нажимают ещё на двух экранах
 */
class FavouriteListUseCase @Inject constructor(
    private val favoriteStationRepository: IFavoriteStationRepository
) : IFavouriteListUseCase {

    override suspend fun getRadioStationFavouriteList(): List<RadioStation> =
        favoriteStationRepository.getFavoriteRadioStationList()
}