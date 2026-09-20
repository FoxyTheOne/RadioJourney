package com.myproject.radiojourney.data.localDatabaseRoom

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myproject.radiojourney.data.localDatabaseRoom.entity.MyStationLocal
import kotlinx.coroutines.flow.Flow

/**
 * DAO своих станций ("Мои радиостанции").
 *
 * Список отдаётся как Flow: экран подписывается один раз, и после добавления или удаления станции
 * Room сам пришлёт новый список - обновлять его руками не нужно
 */
@Dao
interface IMyStationDAO {
    @Query("SELECT * FROM MyStationLocal ORDER BY addedAt")
    fun getMyStationList(): Flow<List<MyStationLocal>>

    // Тот же список разово - для плеера, когда он собирает плейлист
    @Query("SELECT * FROM MyStationLocal ORDER BY addedAt")
    suspend fun getMyStationListOnce(): List<MyStationLocal>

    // Одинаковый адрес потока добавить дважды нельзя (экран проверяет это заранее и показывает ошибку)
    @Query("SELECT EXISTS(SELECT 1 FROM MyStationLocal WHERE url_resolved = :urlResolved)")
    suspend fun hasStationWithUrl(urlResolved: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMyStation(myStation: MyStationLocal)

    @Query("DELETE FROM MyStationLocal WHERE stationuuid = :stationUuid")
    suspend fun deleteMyStation(stationUuid: String)
}