package com.myproject.radiojourney.data.localDatabaseRoom

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.myproject.radiojourney.data.localDatabaseRoom.entity.SavedStationLocal

/**
 * DAO сохранённых списков станций (см. SavedStationLocal)
 */
@Dao
interface ISavedStationDAO {
    @Query("SELECT * FROM SavedStationLocal WHERE countryCode = :countryCode ORDER BY position")
    suspend fun getStationList(countryCode: String): List<SavedStationLocal>

    @Query("DELETE FROM SavedStationLocal WHERE countryCode = :countryCode")
    suspend fun deleteStationList(countryCode: String)

    @Insert
    suspend fun insertStations(stations: List<SavedStationLocal>)

    // Новый список страны целиком заменяет старый. @Transaction - удаление и вставка выполняются вместе:
    // если приложение закроют посередине, в базе останется старый список, а не половина нового
    @Transaction
    suspend fun replaceStationList(countryCode: String, stations: List<SavedStationLocal>) {
        deleteStationList(countryCode)
        insertStations(stations)
    }
}