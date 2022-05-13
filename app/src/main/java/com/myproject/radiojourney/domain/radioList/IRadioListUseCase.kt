package com.myproject.radiojourney.domain.radioList

import com.myproject.radiojourney.entities.presentation.RadioStationPresentation

interface IRadioListUseCase {
    suspend fun getRadioStationList(countryCode: String): List<RadioStationPresentation>
}