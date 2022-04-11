package com.myproject.radiojourney.domain.signUp

import com.myproject.radiojourney.domain.iRepository.IAuthRepository
import javax.inject.Inject

/**
 * Interactor. Domain layer. Работает только с Repository.
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor)
 * При работе с model, здесь происходит преобразование local -> presentation (опционально)
 */
class SignUpInteractor @Inject constructor(
    private val authRepository: IAuthRepository
) : ISignUpInteractor {
    override suspend fun checkEmail(email: String): Boolean = authRepository.checkEmail(email)

    override suspend fun checkPassword(password: String, confirmPassword: String): Boolean =
        authRepository.checkPassword(password, confirmPassword)

    override suspend fun registerNewUser(email: String, password: String) =
        authRepository.registerNewUser(email, password)
}