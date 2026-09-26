package com.myproject.radiojourney.data.localDatabaseRoom.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Таблица с почтой и паролем от удалённых экранов регистрации и входа. Приложением не используется.
 *
 * Оставлена нарочно: удаление таблицы меняет схему базы, а значит, нужна миграция для тех,
 * у кого приложение уже установлено. Ради пустой таблицы это не стоит делать - она никому не мешает
 */
@Entity
class UserEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Int? = null,
    @ColumnInfo(name = "email") val email: String,
    @ColumnInfo(name = "password") val password: String,
)