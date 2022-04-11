package com.myproject.radiojourney.data.localDatabaseRoom

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myproject.radiojourney.model.local.RadioStationLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface IRadioStationDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRadioStationList(vararg radioStationLocalList: RadioStationLocal)

    @Query("SELECT * from RadioStationLocal WHERE url LIKE:url")
    suspend fun getRadioStation(url: String): RadioStationLocal?

    @Query("SELECT * from RadioStationLocal WHERE countrycode LIKE:countryCode")
    fun getRadioStationList(countryCode: String): Flow<List<RadioStationLocal>>
}