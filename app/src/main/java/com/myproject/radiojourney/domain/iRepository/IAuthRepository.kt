package com.myproject.radiojourney.domain.iRepository

interface IAuthRepository {
    suspend fun onLoginClicked()
    suspend fun getToken(): Int?
    fun logout()
}