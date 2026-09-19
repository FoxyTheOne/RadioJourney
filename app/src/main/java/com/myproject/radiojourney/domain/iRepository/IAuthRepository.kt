package com.myproject.radiojourney.domain.iRepository

interface IAuthRepository {
    suspend fun onLoginClicked()

    fun isLoggedIn(): Boolean
    fun logout()
}