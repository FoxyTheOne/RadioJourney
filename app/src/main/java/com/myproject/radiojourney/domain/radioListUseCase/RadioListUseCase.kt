package com.myproject.radiojourney.domain.radioListUseCase

import com.myproject.radiojourney.domain.iRepository.IMainRadioStationRepository
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import com.myproject.radiojourney.other.Constants
import com.myproject.radiojourney.other.Resource
import com.myproject.radiojourney.other.Status
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin. Работает только с Repository.
 *
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor).
 * При работе с model, здесь происходит преобразование local -> presentation, т.е.
 * преобразование моделей в модели нижнего уровня перед тем, как нижний уровень сможет их использовать.
 */
class RadioListUseCase @Inject constructor(
    private val mainRadioStationRepository: IMainRadioStationRepository
) : IRadioListUseCase {
    // Список радиостанций страны с сервера. Если была ошибка сервера - Resource.error
    override suspend fun getRadioStationList(countryCode: String): Resource<List<RadioStationPresentation>> {
        val radioStationLocalListResource =
            mainRadioStationRepository.getRadioStationList(countryCode)
        val radioStationLocalList = radioStationLocalListResource.data

        // Преобразуем модельки local -> presentation
        return if (radioStationLocalListResource.status == Status.SUCCESS && radioStationLocalList != null) {
            Resource.success(radioStationLocalList.map {
                RadioStationPresentation.fromLocalToPresentation(
                    it
                )
            })
        } else {
            Resource.error(Constants.SERVER_IS_DOWN, listOf())
        }
    }
}