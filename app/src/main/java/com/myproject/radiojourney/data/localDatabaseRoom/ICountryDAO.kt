package com.myproject.radiojourney.data.localDatabaseRoom

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myproject.radiojourney.model.local.CountryLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface ICountryDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCountryList(vararg countryLocalList: CountryLocal)

    @Query("SELECT * from CountryLocal WHERE countryCode LIKE:countryCode")
    suspend fun getCountry(countryCode: String): CountryLocal?

    @Query("SELECT * from CountryLocal")
    fun getCountryList(): Flow<List<CountryLocal>>
}