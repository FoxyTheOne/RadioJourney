package com.myproject.radiojourney.data.dataSource.local.favorite

import com.myproject.radiojourney.data.localDatabaseRoom.entity.RadioStationLocal

interface ILocalFavoriteDataSource {
    suspend fun getFavoriteRadioStationList(isStationInFavorite: Boolean): List<RadioStationLocal>
}