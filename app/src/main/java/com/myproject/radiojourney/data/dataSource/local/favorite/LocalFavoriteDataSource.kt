package com.myproject.radiojourney.data.dataSource.local.favorite

import com.myproject.radiojourney.data.localDatabaseRoom.IRadioStationDAO
import com.myproject.radiojourney.entities.local.RadioStationLocal
import javax.inject.Inject

/**
 * Data layer, Local data source. Список избранного из Room
 */
class LocalFavoriteDataSource @Inject constructor(
    private val radioStationDAO: IRadioStationDAO
) : ILocalFavoriteDataSource {
    override suspend fun getFavoriteRadioStationList(isStationInFavorite: Boolean): List<RadioStationLocal> =
        radioStationDAO.getFavoriteRadioStationList(isStationInFavorite)
}