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

    // Чья это ссылка: одинаковый адрес потока добавить дважды нельзя. Возвращаем uuid, а не "да/нет",
    // чтобы при редактировании станция не считала дублем саму себя
    @Query("SELECT stationuuid FROM MyStationLocal WHERE url_resolved = :urlResolved LIMIT 1")
    suspend fun findStationUuidByUrl(urlResolved: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMyStation(myStation: MyStationLocal)

    // Правка станции: uuid и время добавления не трогаем - станция остаётся на своём месте в списке,
    // а плеер по-прежнему узнаёт её по uuid
    @Query("UPDATE MyStationLocal SET stationName = :stationName, url_resolved = :urlResolved WHERE stationuuid = :stationUuid")
    suspend fun updateMyStation(stationUuid: String, stationName: String, urlResolved: String)

    @Query("DELETE FROM MyStationLocal WHERE stationuuid = :stationUuid")
    suspend fun deleteMyStation(stationUuid: String)
}