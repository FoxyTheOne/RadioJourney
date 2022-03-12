package com.myproject.radiojourney.domain.iRepository

import com.myproject.radiojourney.model.local.CountryLocal
import kotlinx.coroutines.flow.Flow

/**
 * Repository. Domain layer.
 */
interface IContentRepository {
    suspend fun getCountryListAndSaveToRoom()
    fun subscribeOnCountryList(): Flow<List<CountryLocal>>
}