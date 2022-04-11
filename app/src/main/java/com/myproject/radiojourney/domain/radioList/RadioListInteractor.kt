package com.myproject.radiojourney.domain.radioList

import com.myproject.radiojourney.domain.iRepository.IContentRepository
import com.myproject.radiojourney.model.presentation.RadioStationPresentation
import javax.inject.Inject

/**
 * Interactor. Domain layer. Работает только с Repository.
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor)
 * При работе с model, здесь происходит преобразование local -> presentation (опционально)
 */
class RadioListInteractor @Inject constructor(
    private val contentRepository: IContentRepository
) : IRadioListInteractor {
    override suspend fun getRadioStationList(countryCode: String): List<RadioStationPresentation> {
        val radioStationLocalList = contentRepository.getRadioStationList(countryCode)

        // Преобразуем модельки local -> presentation
        val radioStationPresentationList = mutableListOf<RadioStationPresentation>()

        radioStationLocalList.forEach { radioStationLocal ->
            val radioStationPresentation =
                RadioStationPresentation.fromLocalToPresentation(radioStationLocal)
            radioStationPresentationList.add(radioStationPresentation)
        }

        return radioStationPresentationList.toList()
    }
}