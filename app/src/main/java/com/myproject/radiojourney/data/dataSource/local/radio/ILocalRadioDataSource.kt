package com.myproject.radiojourney.data.dataSource.local.radio

import com.myproject.radiojourney.model.local.CountryLocal
import kotlinx.coroutines.flow.Flow

interface ILocalRadioDataSource {
    suspend fun saveCountryList(countryLocalList: MutableList<CountryLocal>)
    fun subscribeOnCountryList(): Flow<List<CountryLocal>>
}