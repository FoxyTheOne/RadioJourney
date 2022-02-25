package com.myproject.radiojourney.data.localDatabaseRoom

import androidx.room.Database
import androidx.room.RoomDatabase
import com.myproject.radiojourney.model.local.UserEntity

@Database(
        entities = [UserEntity::class],
        version = 1,
        exportSchema = false
    )
    abstract class AppRoomDBAbstract : RoomDatabase() {
        abstract fun getUserDAO(): IUserDAO
    }