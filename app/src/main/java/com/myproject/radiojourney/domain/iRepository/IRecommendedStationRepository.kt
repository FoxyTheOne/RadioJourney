package com.myproject.radiojourney.domain.iRepository

import com.myproject.radiojourney.model.local.RadioStationLocal

interface IRecommendedStationRepository {
    suspend fun getRecommendedRadioStationList(): List<RadioStationLocal>
}