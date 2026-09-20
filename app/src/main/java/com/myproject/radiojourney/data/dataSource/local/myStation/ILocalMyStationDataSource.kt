package com.myproject.radiojourney.data.dataSource.local.myStation

import com.myproject.radiojourney.data.localDatabaseRoom.entity.MyStationLocal
import kotlinx.coroutines.flow.Flow

/**
 * Local data source своих станций: таблица MyStationLocal в Room
 */
interface ILocalMyStationDataSource {
    fun getMyStationList(): Flow<List<MyStationLocal>>

    suspend fun getMyStationListOnce(): List<MyStationLocal>

    suspend fun hasStationWithUrl(urlResolved: String): Boolean

    suspend fun saveMyStation(myStation: MyStationLocal)

    suspend fun deleteMyStation(stationUuid: String)
}