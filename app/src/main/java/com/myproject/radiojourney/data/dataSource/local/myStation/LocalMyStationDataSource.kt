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

    override suspend fun getMyStationListOnce(): List<MyStationLocal> =
        myStationDAO.getMyStationListOnce()

    override suspend fun findStationUuidByUrl(urlResolved: String): String? =
        myStationDAO.findStationUuidByUrl(urlResolved)

    override suspend fun saveMyStation(myStation: MyStationLocal) =
        myStationDAO.saveMyStation(myStation)

    override suspend fun updateMyStation(
        stationUuid: String,
        stationName: String,
        urlResolved: String
    ) =
        myStationDAO.updateMyStation(stationUuid, stationName, urlResolved)

    override suspend fun deleteMyStation(stationUuid: String) =
        myStationDAO.deleteMyStation(stationUuid)
}