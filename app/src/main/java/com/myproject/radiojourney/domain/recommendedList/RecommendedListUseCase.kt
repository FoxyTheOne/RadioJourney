package com.myproject.radiojourney.domain.recommendedList

import com.myproject.radiojourney.domain.iRepository.IRadioStationRepository
import com.myproject.radiojourney.domain.iRepository.IRecommendedStationRepository
import com.myproject.radiojourney.model.local.RadioStationLocal
import com.myproject.radiojourney.model.presentation.RadioStationPresentation
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin. Работает только с Repository.
 *
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor).
 * При работе с model, здесь происходит преобразование local -> presentation, т.е.
 * преобразование моделей в модели нижнего уровня перед тем, как нижний уровень сможет их использовать.
 */
class RecommendedListUseCase @Inject constructor(
    private val recommendedStationRepository: IRecommendedStationRepository,
    private val radioStationRepository: IRadioStationRepository
) : IRecommendedListUseCase {

    override suspend fun getRadioStationRecommendedList(): List<RadioStationPresentation> {
        // Получаем из репозитория List<RadioStationLocal>
        val recommendedRadioStationLocalList =
            recommendedStationRepository.getRecommendedRadioStationList()

        // local -> presentation
        val recommendedRadioStationPresentationList = mutableListOf<RadioStationPresentation>()
        recommendedRadioStationLocalList.forEach { radioStationLocal ->
            val radioStationRecommendedPresentation =
                RadioStationPresentation.fromLocalToPresentation(radioStationLocal)
            recommendedRadioStationPresentationList.add(radioStationRecommendedPresentation)
        }

        return recommendedRadioStationPresentationList.sortedWith(compareBy { it.countryCode }) // Отсортируем список по странам перед отображением
    }

    override suspend fun addStationInRoomToFavourites(currentRadioStation: RadioStationPresentation) {
        val currentRadioStationLocal = RadioStationLocal.fromPresentationToLocal(
            currentRadioStation,
            isStationInFavourite = true
        )
        radioStationRepository.saveRadioStationInRoom(currentRadioStationLocal)
    }

    override suspend fun deleteStationInRoomFromFavourite(currentRadioStation: RadioStationPresentation) {
        val currentRadioStationLocal = RadioStationLocal.fromPresentationToLocal(
            currentRadioStation,
            isStationInFavourite = false
        )
        radioStationRepository.saveRadioStationInRoom(currentRadioStationLocal)
    }

}