package com.myproject.radiojourney.domain.signUp

import com.myproject.radiojourney.domain.iRepository.IAuthRepository
import javax.inject.Inject

class SignUpInteractor @Inject constructor(
    private val authRepository: IAuthRepository
) : ISignUpInteractor {
    override suspend fun checkEmail(email: String): Boolean = authRepository.checkEmail(email)

    override fun checkPassword(password: String, confirmPassword: String): Boolean = authRepository.checkPassword(password, confirmPassword)

    override suspend fun registerNewUser(email: String, password: String) = authRepository.registerNewUser(email, password)
}