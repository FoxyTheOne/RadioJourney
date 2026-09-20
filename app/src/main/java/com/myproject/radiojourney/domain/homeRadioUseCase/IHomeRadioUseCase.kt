package com.myproject.radiojourney.domain.homeRadioUseCase

import com.myproject.radiojourney.domain.model.Country
import kotlinx.coroutines.flow.Flow

/**
 * UseCase главного экрана: страны для маркеров на карте и состояние информационного блока
 */
interface IHomeRadioUseCase {
    fun subscribeOnCountryList(): Flow<List<Country>>

    suspend fun setIsHideInfoClicked(isHideInfoClicked: Boolean)
    fun isHideInfoClicked(): Flow<Boolean>
}