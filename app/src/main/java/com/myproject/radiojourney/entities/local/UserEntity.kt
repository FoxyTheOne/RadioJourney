package com.myproject.radiojourney.entities.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Класс для сохранения в базе данных Room почты и пароля пользователей
 *
 * Не нужен после удаления экранов SignIn и SignUp
 */
@Entity
class UserEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Int? = null,
    @ColumnInfo(name = "email") val email: String,
    @ColumnInfo(name = "password") val password: String,
)