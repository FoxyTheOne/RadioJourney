package com.myproject.radiojourney.domain.signIn

import com.myproject.radiojourney.domain.iRepository.IAuthRepository
import javax.inject.Inject

/**
 * Interactor. Domain layer. Работает только с Repository.
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor)
 * При работе с model, здесь происходит преобразование local -> presentation (опционально)
 */
class SignInInteractor @Inject constructor(
    private val authRepository: IAuthRepository
) : ISignInInteractor {
    override suspend fun isRememberLoginAndPasswordSelected() =
        authRepository.isRememberLoginAndPasswordSelected()

    override suspend fun getEmail(): String? = authRepository.getEmail()
    override suspend fun getPassword(): String? = authRepository.getPassword()

    override suspend fun onLoginClicked(emailText: String, passwordText: String): Boolean =
        authRepository.onLoginClicked(emailText, passwordText)

    // Каждый раз, когда мы кликаем, будет исполняться этот метод. Здесь мы сохраняем статус check box
    override suspend fun setRememberLoginAndPasswordSelectedOrNot(isSelected: Boolean) =
        authRepository.setRememberLoginAndPasswordSelectedOrNot(isSelected)

}