package com.myproject.radiojourney.domain.favouriteListUseCase

import com.myproject.radiojourney.domain.iRepository.IFavoriteStationRepository
import com.myproject.radiojourney.domain.iRepository.IMainRadioStationRepository
import com.myproject.radiojourney.entities.local.RadioStationLocal
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin. Работает только с Repository.
 *
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor).
 * При работе с model, здесь происходит преобразование local -> presentation, т.е.
 * преобразование моделей в модели нижнего уровня перед тем, как нижний уровень сможет их использовать.
 */
class FavouriteListUseCase @Inject constructor(
    private val favoriteStationRepository: IFavoriteStationRepository,
    private val mainRadioStationRepository: IMainRadioStationRepository
) : IFavouriteListUseCase {
    override suspend fun getRadioStationFavouriteList(isStationInFavorite: Boolean): List<RadioStationPresentation> {
        // Получаем из репозитория список local
        val radioStationFavouriteListLocal =
            favoriteStationRepository.getFavoriteRadioStationList(isStationInFavorite)

        // local -> presentation
        val radioStationFavouriteListPresentation = mutableListOf<RadioStationPresentation>()
        radioStationFavouriteListLocal.forEach {
            val radioStationPresentation = RadioStationPresentation.fromLocalToPresentation(it)
            radioStationFavouriteListPresentation.add(radioStationPresentation)
        }

        return radioStationFavouriteListPresentation
    }

    override suspend fun addStationInRoomToFavourites(currentRadioStation: RadioStationPresentation) {
        val currentRadioStationLocal = RadioStationLocal.fromPresentationToLocal(
            currentRadioStation,
            isStationInFavourite = true
        )
        mainRadioStationRepository.saveRadioStationInRoom(currentRadioStationLocal)
    }

    override suspend fun deleteStationInRoomFromFavourite(currentRadioStation: RadioStationPresentation) {
        val currentRadioStationLocal = RadioStationLocal.fromPresentationToLocal(
            currentRadioStation,
            isStationInFavourite = false
        )
        mainRadioStationRepository.saveRadioStationInRoom(currentRadioStationLocal)
    }
}