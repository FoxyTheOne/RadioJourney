package com.myproject.radiojourney.domain.radioListUseCase

import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import com.myproject.radiojourney.other.Resource

interface IRadioListUseCase {
    suspend fun getRadioStationList(countryCode: String): Resource<List<RadioStationPresentation>>
}