package com.myproject.radiojourney.data.dataSource.local

import androidx.lifecycle.MutableLiveData

interface ILocalAuthDataSource {
    suspend fun isRememberLoginAndPasswordSelected(): Boolean
    suspend fun getEmail(): String?
    suspend fun getPassword(): String?
//    fun getEmailIfCheckBoxSelected(): MutableLiveData<String>
//    fun getPasswordIfCheckBoxSelected(): MutableLiveData<String>
//    fun isCheckBoxSelected(): MutableLiveData<Boolean>

    suspend fun onLoginClicked(emailText: String, passwordText: String): Boolean

    // Каждый раз, когда мы кликаем, будет исполняться этот метод. Здесь мы сохраняем статус check box
    suspend fun setRememberLoginAndPasswordSelectedOrNot(isSelected: Boolean)

    suspend fun checkEmail(email: String): Boolean
    fun checkPassword(password: String, confirmPassword: String): Boolean
    suspend fun registerNewUser(email: String, password: String)

    fun logout()

}