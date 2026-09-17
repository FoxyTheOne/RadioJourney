package com.myproject.radiojourney.data.dataSource.local.auth

interface ILocalUserDataSource {
    suspend fun onLoginClicked()
    suspend fun getToken(): String

    // Пользователь уже входил (есть токен и это не первый запуск) - первый экран пропускаем
    fun isLoggedIn(): Boolean

    fun logout()
}