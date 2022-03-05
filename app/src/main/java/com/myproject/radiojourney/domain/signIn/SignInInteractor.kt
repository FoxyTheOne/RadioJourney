package com.myproject.radiojourney.domain.signIn

import androidx.lifecycle.MutableLiveData
import com.myproject.radiojourney.domain.iRepository.IAuthRepository
import javax.inject.Inject

/**
 * Interactor работает с Repository
 */
class SignInInteractor @Inject constructor(
    private val authRepository: IAuthRepository
) : ISignInInteractor {
    override suspend fun isRememberLoginAndPasswordSelected() = authRepository.isRememberLoginAndPasswordSelected()
    override suspend fun getEmail(): String? = authRepository.getEmail()
    override suspend fun getPassword(): String? = authRepository.getPassword()

//    override fun getEmailIfCheckBoxSelected(): MutableLiveData<String> =
//        authRepository.getEmailIfCheckBoxSelected()
//
//    override fun getPasswordIfCheckBoxSelected(): MutableLiveData<String> =
//        authRepository.getPasswordIfCheckBoxSelected()
//
//    override fun isCheckBoxSelected(): MutableLiveData<Boolean> =
//        authRepository.isCheckBoxSelected()

    override suspend fun onLoginClicked(emailText: String, passwordText: String): Boolean =
        authRepository.onLoginClicked(emailText, passwordText)

    // Каждый раз, когда мы кликаем, будет исполняться этот метод. Здесь мы сохраняем статус check box
    override suspend fun setRememberLoginAndPasswordSelectedOrNot(isSelected: Boolean) =
        authRepository.setRememberLoginAndPasswordSelectedOrNot(isSelected)

}