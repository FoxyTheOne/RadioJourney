package com.myproject.radiojourney.data.localDatabaseRoom

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myproject.radiojourney.model.local.UserEntity

@Dao
interface IUserDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUser(user: UserEntity)

    @Query("SELECT * from UserEntity WHERE email LIKE:email")
    suspend fun getUser(email: String): UserEntity?
}