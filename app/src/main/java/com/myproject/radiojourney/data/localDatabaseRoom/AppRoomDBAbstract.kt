package com.myproject.radiojourney.data.localDatabaseRoom

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import com.myproject.radiojourney.entities.local.CountryLocal
import com.myproject.radiojourney.entities.local.RadioStationLocal
import com.myproject.radiojourney.entities.local.UserEntity

@Database(
    entities = [UserEntity::class, CountryLocal::class, RadioStationLocal::class],
    version = 12,
    exportSchema = true,
//    autoMigrations = [
//        AutoMigration(from = 11, to = 12)
//    ]
)
@TypeConverters(LatLngConverter::class)
abstract class AppRoomDBAbstract : RoomDatabase() {
    abstract fun getUserDAO(): IUserDAO
    abstract fun getCountryDAO(): ICountryDAO
    abstract fun getRadioStationDAO(): IRadioStationDAO

    // If you just added a column - just add a defaultValue in column info for auto migration

    // If you renamed a column
//    @RenameColumn(tableName = "User", fromColumnName = "smth", toColumnName = "smth2")
//    class Migration2to3:AutoMigrationSpec
    // And add this class in [] like this: AutoMigration(from = 2, to = 3, spec = UserDatabase.Migration2to3::class)
}