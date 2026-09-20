package com.myproject.radiojourney.data.dataSource.local.favorite

import com.myproject.radiojourney.data.localDatabaseRoom.entity.RadioStationLocal

/**
 * Local data source избранного: чтение станций, отмеченных звездой, из Room
 */
interface ILocalFavoriteDataSource {
    suspend fun getFavoriteRadioStationList(isStationInFavorite: Boolean): List<RadioStationLocal>
}