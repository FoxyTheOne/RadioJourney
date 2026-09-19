package com.myproject.radiojourney.domain.homeRadioUseCase

import com.myproject.radiojourney.entities.presentation.CountryPresentation
import kotlinx.coroutines.flow.Flow

interface IHomeRadioUseCase {
    fun subscribeOnCountryList(): Flow<List<CountryPresentation>>

    suspend fun setIsHideInfoClicked(isHideInfoClicked: Boolean)
    suspend fun isHideInfoClicked(): Boolean
}