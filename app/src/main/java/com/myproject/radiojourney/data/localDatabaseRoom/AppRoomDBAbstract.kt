package com.myproject.radiojourney.data.localDatabaseRoom

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.myproject.radiojourney.model.local.CountryLocal
import com.myproject.radiojourney.model.local.UserEntity

@Database(
    entities = [UserEntity::class, CountryLocal::class],
    version = 2,
    exportSchema = true,
//    autoMigrations = [
//        AutoMigration(from = 1, to = 2)
//    ]
)
@TypeConverters(LatLngConverter::class)
abstract class AppRoomDBAbstract : RoomDatabase() {
    abstract fun getUserDAO(): IUserDAO
    abstract fun getCountryDAO(): ICountryDAO
}