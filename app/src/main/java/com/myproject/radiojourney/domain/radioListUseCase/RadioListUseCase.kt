package com.myproject.radiojourney.domain.radioListUseCase

import com.myproject.radiojourney.domain.iRepository.IMainRadioStationRepository
import com.myproject.radiojourney.domain.model.RadioStationList
import com.myproject.radiojourney.other.Resource
import javax.inject.Inject

/**
 * Domain layer, UseCase экрана со списком станций страны.
 *
 * Своей логики у него нет - он просто просит список у репозитория. Класс всё равно оставлен:
 * ViewModel обращается к domain, а не к data напрямую, и если правила появятся, их будет куда добавить
 */
class RadioListUseCase @Inject constructor(
    private val mainRadioStationRepository: IMainRadioStationRepository
) : IRadioListUseCase {

    // Список радиостанций страны с сервера. Если была ошибка сервера - Resource.error
    override suspend fun getRadioStationList(countryCode: String): Resource<RadioStationList> =
        mainRadioStationRepository.getRadioStationList(countryCode)
}