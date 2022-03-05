package com.myproject.radiojourney.domain.signUp

interface ISignUpInteractor {
    suspend fun checkEmail(email: String): Boolean
    suspend fun checkPassword(password: String, confirmPassword: String): Boolean
    suspend fun registerNewUser(email: String, password: String)
}