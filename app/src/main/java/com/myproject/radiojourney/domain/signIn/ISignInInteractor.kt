package com.myproject.radiojourney.domain.signIn

interface ISignInInteractor {

    suspend fun isRememberLoginAndPasswordSelected(): Boolean
    suspend fun getEmail(): String?
    suspend fun getPassword(): String?

    suspend fun onLoginClicked(emailText: String, passwordText: String): Boolean

    // Каждый раз, когда мы кликаем, будет исполняться этот метод. Здесь мы сохраняем статус check box
    suspend fun setRememberLoginAndPasswordSelectedOrNot(isSelected: Boolean)

}