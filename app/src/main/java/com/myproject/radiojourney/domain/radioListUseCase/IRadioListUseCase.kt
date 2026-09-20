package com.myproject.radiojourney.domain.radioListUseCase

import com.myproject.radiojourney.domain.model.RadioStation
import com.myproject.radiojourney.other.Resource

interface IRadioListUseCase {
    // Список радиостанций страны с сервера
    suspend fun getRadioStationList(countryCode: String): Resource<List<RadioStation>>
}