package com.myproject.radiojourney.domain.homeRadioUseCase

import com.myproject.radiojourney.domain.model.Country
import kotlinx.coroutines.flow.Flow

interface IHomeRadioUseCase {
    fun subscribeOnCountryList(): Flow<List<Country>>

    suspend fun setIsHideInfoClicked(isHideInfoClicked: Boolean)
    suspend fun isHideInfoClicked(): Boolean
}