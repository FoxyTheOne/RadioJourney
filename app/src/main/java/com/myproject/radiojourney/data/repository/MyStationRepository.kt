package com.myproject.radiojourney.data.repository

import com.myproject.radiojourney.data.dataSource.local.myStation.ILocalMyStationDataSource
import com.myproject.radiojourney.data.mapper.toDomain
import com.myproject.radiojourney.data.mapper.toMyStationLocal
import com.myproject.radiojourney.domain.iRepository.IMyStationRepository
import com.myproject.radiojourney.domain.model.RadioStation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Data layer, Repository своих станций. Работает с Local data source и преобразует local -> domain
 */
class MyStationRepository @Inject constructor(
    private val localMyStationDataSource: ILocalMyStationDataSource
) : IMyStationRepository {

    override fun getMyStationList(): Flow<List<RadioStation>> =
        localMyStationDataSource.getMyStationList()
            .map { stations -> stations.map { it.toDomain() } }

    override suspend fun getMyStationListOnce(): List<RadioStation> =
        localMyStationDataSource.getMyStationListOnce().map { it.toDomain() }

    override suspend fun findStationUuidByUrl(urlResolved: String): String? =
        localMyStationDataSource.findStationUuidByUrl(urlResolved)

    override suspend fun saveMyStation(station: RadioStation) =
        localMyStationDataSource.saveMyStation(station.toMyStationLocal())

    override suspend fun updateMyStation(stationUuid: String, name: String, urlResolved: String) =
        localMyStationDataSource.updateMyStation(stationUuid, name, urlResolved)

    override suspend fun deleteMyStation(stationUuid: String) =
        localMyStationDataSource.deleteMyStation(stationUuid)
}