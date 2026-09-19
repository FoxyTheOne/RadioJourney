package com.myproject.radiojourney.data.dataSource.local.favorite

import com.myproject.radiojourney.entities.local.RadioStationLocal

interface ILocalFavoriteDataSource {
    suspend fun getFavoriteRadioStationList(isStationInFavorite: Boolean): List<RadioStationLocal>
}