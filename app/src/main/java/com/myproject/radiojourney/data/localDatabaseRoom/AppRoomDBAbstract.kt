package com.myproject.radiojourney.data.localDatabaseRoom

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.myproject.radiojourney.data.localDatabaseRoom.entity.CountryLocal
import com.myproject.radiojourney.data.localDatabaseRoom.entity.MyStationLocal
import com.myproject.radiojourney.data.localDatabaseRoom.entity.RadioStationLocal
import com.myproject.radiojourney.data.localDatabaseRoom.entity.SavedStationLocal
import com.myproject.radiojourney.data.localDatabaseRoom.entity.UserEntity

/**
 * База данных Room. Таблицы: страны для карты (CountryLocal), избранные станции (RadioStationLocal),
 * свои станции пользователя (MyStationLocal) и сохранённые списки станций по странам (SavedStationLocal).
 *
 * @Database перечисляет таблицы и версию базы, @TypeConverters - классы, которые Room сам не умеет хранить
 * (здесь LatLng от Google Maps). Экземпляр создаётся один раз в DataModule.
 *
 * Переиспользование: минимальный пример базы Room - класс базы, DAO, entity и конвертер
 */
@Database(
    entities = [UserEntity::class, CountryLocal::class, RadioStationLocal::class, MyStationLocal::class, SavedStationLocal::class],
    version = 14,
    exportSchema = true,
)
@TypeConverters(LatLngConverter::class)
// UserEntity (таблица от удалённых экранов входа) оставлена в @Database: удаление таблицы меняет схему базы и требует миграции
abstract class AppRoomDBAbstract : RoomDatabase() {
    abstract fun getCountryDAO(): ICountryDAO
    abstract fun getRadioStationDAO(): IRadioStationDAO
    abstract fun getMyStationDAO(): IMyStationDAO
    abstract fun getSavedStationDAO(): ISavedStationDAO

    // If you just added a column - just add a defaultValue in column info for auto migration

    // If you renamed a column
//    @RenameColumn(tableName = "User", fromColumnName = "smth", toColumnName = "smth2")
//    class Migration2to3:AutoMigrationSpec
    // And add this class in [] like this: AutoMigration(from = 2, to = 3, spec = UserDatabase.Migration2to3::class)
}