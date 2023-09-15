package com.myproject.radiojourney.data.localDatabaseRoom

import androidx.room.*
import com.myproject.radiojourney.entities.local.RadioStationLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface IRadioStationDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRadioStationList(vararg radioStationLocalList: RadioStationLocal)

    @Query("SELECT * from RadioStationLocal WHERE stationuuid LIKE:stationUuid")
    suspend fun getRadioStationByUuid(stationUuid: String): RadioStationLocal?

    @Query("SELECT * from RadioStationLocal WHERE url_resolved LIKE:urlResolved")
    suspend fun getRadioStationByUrl(urlResolved: String): RadioStationLocal?
    // ^ Могут быть сохранены в локальную базу несколько одинаковых станций с разным stationUuid. В таком случае могут возникнуть ошибки, в зависимости от того, какая станция прилетит по запросу

    @Query("SELECT * from RadioStationLocal WHERE countrycode LIKE:countryCode")
    fun getRadioStationList(countryCode: String): Flow<List<RadioStationLocal>>

    @Query("SELECT * from RadioStationLocal WHERE isStationInRecommended LIKE:isStationInRecommended")
    fun getRecommendedRadioStationList(isStationInRecommended: Boolean): List<RadioStationLocal>

    @Query("SELECT * from RadioStationLocal WHERE isStationInFavourite LIKE:isStationInFavorite")
    fun getFavoriteRadioStationList(isStationInFavorite: Boolean): List<RadioStationLocal>
}