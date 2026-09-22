package com.myproject.radiojourney.domain.radioListUseCase

import com.myproject.radiojourney.domain.model.RadioStationList
import com.myproject.radiojourney.other.Resource

/**
 * UseCase экрана со списком станций страны
 */
interface IRadioListUseCase {
    // Список радиостанций страны с сервера
    suspend fun getRadioStationList(countryCode: String): Resource<RadioStationList>
}