package com.myproject.radiojourney.domain.radioList

import com.myproject.radiojourney.domain.iRepository.IRadioStationRepository
import com.myproject.radiojourney.model.presentation.RadioStationPresentation
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin. Работает только с Repository.
 *
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor).
 * При работе с model, здесь происходит преобразование local -> presentation, т.е.
 * преобразование моделей в модели нижнего уровня перед тем, как нижний уровень сможет их использовать.
 */
class RadioListUseCase @Inject constructor(
    private val radioStationRepository: IRadioStationRepository
) : IRadioListUseCase {
    override suspend fun getRadioStationList(countryCode: String): List<RadioStationPresentation> {
        val radioStationLocalList = radioStationRepository.getRadioStationList(countryCode)

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