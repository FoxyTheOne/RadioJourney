package com.myproject.radiojourney.domain.favouriteListUseCase

import com.myproject.radiojourney.domain.iRepository.IFavoriteStationRepository
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin. Работает только с Repository.
 *
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor).
 * При работе с model, здесь происходит преобразование local -> presentation.
 * Добавление в избранное и удаление из него - в общем ChangeFavouriteUseCase
 */
class FavouriteListUseCase @Inject constructor(
    private val favoriteStationRepository: IFavoriteStationRepository
) : IFavouriteListUseCase {
    override suspend fun getRadioStationFavouriteList(): List<RadioStationPresentation> =
        favoriteStationRepository.getFavoriteRadioStationList(isStationInFavorite = true)
            .map { RadioStationPresentation.fromLocalToPresentation(it) }
}