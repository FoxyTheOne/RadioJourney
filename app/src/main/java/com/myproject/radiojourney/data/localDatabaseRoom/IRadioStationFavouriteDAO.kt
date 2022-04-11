package com.myproject.radiojourney.data.localDatabaseRoom

import androidx.room.*
import com.myproject.radiojourney.model.local.RadioStationFavouriteLocal

@Dao
interface IRadioStationFavouriteDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRadioStationFavourite(radioStationFavourite: RadioStationFavouriteLocal)

    @Delete
    suspend fun deleteRadioStationFromFavourite(vararg radioStationFavourite: RadioStationFavouriteLocal)

    @Query("SELECT * from RadioStationFavouriteLocal WHERE url LIKE:url")
    suspend fun getRadioStationFavourite(url: String): RadioStationFavouriteLocal?
}