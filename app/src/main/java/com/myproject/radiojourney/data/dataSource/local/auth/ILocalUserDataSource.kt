package com.myproject.radiojourney.data.dataSource.local.auth

interface ILocalUserDataSource {
    suspend fun onLoginClicked()
    suspend fun getToken(): String

    fun logout()
}