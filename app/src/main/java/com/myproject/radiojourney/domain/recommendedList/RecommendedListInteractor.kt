package com.myproject.radiojourney.domain.recommendedList

import com.myproject.radiojourney.domain.iRepository.IContentRepository
import com.myproject.radiojourney.model.local.CountryLocal
import com.myproject.radiojourney.model.local.RadioStationFavouriteLocal
import com.myproject.radiojourney.model.presentation.RadioStationPresentation
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Interactor. Domain layer. Работает только с Repository.
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor)
 * При работе с model, здесь происходит преобразование local -> presentation (опционально)
 */
class RecommendedListInteractor @Inject constructor(
    private val contentRepository: IContentRepository
) : IRecommendedListInteractor {
    override suspend fun getToken(): Int? = contentRepository.getToken()

    override suspend fun getRadioStationRecommendedList(): List<RadioStationPresentation> {
        // Получаем из репозитория List<RadioStationLocal>
        val recommendedRadioStationLocalList = contentRepository.getRecommendedRadioStationList()

        // local -> presentation
        val recommendedRadioStationPresentationList = mutableListOf<RadioStationPresentation>()
        recommendedRadioStationLocalList.forEach { radioStationLocal ->
            val radioStationRecommendedPresentation =
                RadioStationPresentation.fromLocalToPresentation(radioStationLocal)
            recommendedRadioStationPresentationList.add(radioStationRecommendedPresentation)
        }

        return recommendedRadioStationPresentationList
    }
}