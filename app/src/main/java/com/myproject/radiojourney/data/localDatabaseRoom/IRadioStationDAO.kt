package com.myproject.radiojourney.data.localDatabaseRoom

import androidx.room.*
import com.myproject.radiojourney.entities.local.RadioStationLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface IRadioStationDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRadioStationList(vararg radioStationLocalList: RadioStationLocal)

    @Query("SELECT * from RadioStationLocal WHERE url LIKE:url")
    suspend fun getRadioStation(url: String): RadioStationLocal?

    @Query("SELECT * from RadioStationLocal WHERE countrycode LIKE:countryCode")
    fun getRadioStationList(countryCode: String): Flow<List<RadioStationLocal>>

    @Query("SELECT * from RadioStationLocal WHERE isStationInRecommended LIKE:isStationInRecommended")
    fun getRecommendedRadioStationList(isStationInRecommended: Boolean): List<RadioStationLocal>

    @Query("SELECT * from RadioStationLocal WHERE isStationInFavourite LIKE:isStationInFavorite")
    fun getFavoriteRadioStationList(isStationInFavorite: Boolean): List<RadioStationLocal>
}