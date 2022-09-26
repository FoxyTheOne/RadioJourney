package com.myproject.radiojourney.data.localDatabaseRoom

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.myproject.radiojourney.entities.local.CountryLocal
import com.myproject.radiojourney.entities.local.RadioStationLocal
import com.myproject.radiojourney.entities.local.UserEntity

@Database(
    entities = [UserEntity::class, CountryLocal::class, RadioStationLocal::class],
    version = 11,
    exportSchema = true,
//    autoMigrations = [
//        AutoMigration(from = 10, to = 11)
//    ]
)
@TypeConverters(LatLngConverter::class)
abstract class AppRoomDBAbstract : RoomDatabase() {
    abstract fun getUserDAO(): IUserDAO
    abstract fun getCountryDAO(): ICountryDAO
    abstract fun getRadioStationDAO(): IRadioStationDAO
}