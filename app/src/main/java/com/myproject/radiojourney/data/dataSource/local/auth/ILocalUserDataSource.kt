package com.myproject.radiojourney.data.dataSource.local.auth

import kotlinx.coroutines.flow.Flow

/**
 * Local data source входа: что приложение знает о пользователе.
 *
 * Интерфейс лежит в data, потому что это деталь хранения. Экраны с ним не работают - они видят только репозиторий
 */
interface ILocalUserDataSource {
    suspend fun onLoginClicked()

    // Пользователь уже входил (есть токен и это не первый запуск) - первый экран пропускаем
    fun isLoggedIn(): Flow<Boolean>
}