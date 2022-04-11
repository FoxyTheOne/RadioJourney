package com.myproject.radiojourney.data.localDatabaseRoom

import androidx.room.*
import com.myproject.radiojourney.model.local.UserEntity
import com.myproject.radiojourney.model.local.UserWithStations

@Dao
interface IUserDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUser(user: UserEntity)

    @Query("SELECT * from UserEntity WHERE email LIKE:email")
    suspend fun getUser(email: String): UserEntity?

    // Finally, add a method to the DAO class that returns all instances of the data class that pairs the parent entity and the child entity.
    // This method requires Room to run two queries, so add the @Transaction annotation to this method to ensure that the whole operation is performed atomically.
    @Transaction
    @Query("SELECT * FROM UserEntity")
    fun getUsersWithStations(): List<UserWithStations>
}