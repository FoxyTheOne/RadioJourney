package com.myproject.radiojourney.domain.iRepository

import com.myproject.radiojourney.entities.local.RadioStationLocal

interface IRecommendedStationRepository {
    suspend fun getRecommendedRadioStationList(): List<RadioStationLocal>
}