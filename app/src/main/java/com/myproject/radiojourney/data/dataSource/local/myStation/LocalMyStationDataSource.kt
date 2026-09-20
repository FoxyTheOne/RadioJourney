package com.myproject.radiojourney.data.dataSource.local.myStation

import com.myproject.radiojourney.data.localDatabaseRoom.IMyStationDAO
import com.myproject.radiojourney.data.localDatabaseRoom.entity.MyStationLocal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Data layer, Local data source. Свои станции пользователя из Room
 */
class LocalMyStationDataSource @Inject constructor(
    private val myStationDAO: IMyStationDAO
) : ILocalMyStationDataSource {

    override fun getMyStationList(): Flow<List<MyStationLocal>> = myStationDAO.getMyStationList()

    override suspend fun getMyStationListOnce(): List<MyStationLocal> = myStationDAO.getMyStationListOnce()

    override suspend fun hasStationWithUrl(urlResolved: String): Boolean = myStationDAO.hasStationWithUrl(urlResolved)

    override suspend fun saveMyStation(myStation: MyStationLocal) = myStationDAO.saveMyStation(myStation)

    override suspend fun deleteMyStation(stationUuid: String) = myStationDAO.deleteMyStation(stationUuid)
}