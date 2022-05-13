package com.myproject.radiojourney.data.dataSource.local.recommended

import com.myproject.radiojourney.entities.local.RadioStationLocal

interface ILocalRecommendedDataSource {
    suspend fun getRecommendedRadioStationList(): List<RadioStationLocal>
}