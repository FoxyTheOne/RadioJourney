package com.myproject.radiojourney.domain.radioList

import com.myproject.radiojourney.model.presentation.RadioStationPresentation

interface IRadioListInteractor {
    suspend fun getRadioStationList(countryCode: String): List<RadioStationPresentation>
}