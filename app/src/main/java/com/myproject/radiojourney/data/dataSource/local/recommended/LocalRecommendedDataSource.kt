package com.myproject.radiojourney.data.dataSource.local.recommended

import com.myproject.radiojourney.data.localDatabaseRoom.IRadioStationDAO
import com.myproject.radiojourney.model.local.RadioStationLocal
import javax.inject.Inject

/**
 * LocalRecommendedDataSource Будет доставать данные, либо сохранять их в локальную базу данных (SharedPreference, Room)
 */
class LocalRecommendedDataSource @Inject constructor(
    private val radioStationDAO: IRadioStationDAO
): ILocalRecommendedDataSource {
    override suspend fun getRecommendedRadioStationList(): List<RadioStationLocal> =
        radioStationDAO.getRecommendedRadioStationList(true)
}