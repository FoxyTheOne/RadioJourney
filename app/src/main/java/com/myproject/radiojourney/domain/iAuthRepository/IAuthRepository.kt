package com.myproject.radiojourney.domain.iAuthRepository

import androidx.lifecycle.MutableLiveData

interface IAuthRepository {

    fun getEmailIfCheckBoxSelected(): MutableLiveData<String>
    fun getPasswordIfCheckBoxSelected(): MutableLiveData<String>
    fun isCheckBoxSelected(): MutableLiveData<Boolean>

    suspend fun onLoginClicked(emailText: String, passwordText: String): Boolean

    // Каждый раз, когда мы кликаем, будет исполняться этот метод. Здесь мы сохраняем статус check box
    suspend fun setRememberLoginAndPasswordSelectedOrNot(isSelected: Boolean)

}