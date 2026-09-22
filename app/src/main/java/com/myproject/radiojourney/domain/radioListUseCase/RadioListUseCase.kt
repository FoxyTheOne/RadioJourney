package com.myproject.radiojourney.domain.radioListUseCase

import com.myproject.radiojourney.domain.iRepository.IMainRadioStationRepository
import com.myproject.radiojourney.domain.model.RadioStationList
import com.myproject.radiojourney.other.Resource
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin. Работает только с Repository.
 */
class RadioListUseCase @Inject constructor(
    private val mainRadioStationRepository: IMainRadioStationRepository
) : IRadioListUseCase {

    // Список радиостанций страны с сервера. Если была ошибка сервера - Resource.error
    override suspend fun getRadioStationList(countryCode: String): Resource<RadioStationList> =
        mainRadioStationRepository.getRadioStationList(countryCode)
}