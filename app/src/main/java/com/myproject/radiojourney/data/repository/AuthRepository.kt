package com.myproject.radiojourney.data.repository

import androidx.lifecycle.MutableLiveData
import com.myproject.radiojourney.data.dataSource.local.ILocalAuthDataSource
import com.myproject.radiojourney.domain.iRepository.IAuthRepository
import javax.inject.Inject

/**
 * Работает с Local и Remote data source
 */
class AuthRepository @Inject constructor(
    private val localAuthDataSource: ILocalAuthDataSource
) : IAuthRepository {
    override suspend fun isRememberLoginAndPasswordSelected(): Boolean = localAuthDataSource.isRememberLoginAndPasswordSelected()
    override suspend fun getEmail(): String? = localAuthDataSource.getEmail()
    override suspend fun getPassword(): String? = localAuthDataSource.getPassword()

//    override fun getEmailIfCheckBoxSelected(): MutableLiveData<String> =
//        localAuthDataSource.getEmailIfCheckBoxSelected()
//
//    override fun getPasswordIfCheckBoxSelected(): MutableLiveData<String> =
//        localAuthDataSource.getPasswordIfCheckBoxSelected()
//
//    override fun isCheckBoxSelected(): MutableLiveData<Boolean> =
//        localAuthDataSource.isCheckBoxSelected()


    override suspend fun onLoginClicked(emailText: String, passwordText: String): Boolean =
        localAuthDataSource.onLoginClicked(emailText, passwordText)

    // Каждый раз, когда мы кликаем, будет исполняться этот метод. Здесь мы сохраняем статус check box
    override suspend fun setRememberLoginAndPasswordSelectedOrNot(isSelected: Boolean) =
        localAuthDataSource.setRememberLoginAndPasswordSelectedOrNot(isSelected)

    override suspend fun checkEmail(email: String): Boolean = localAuthDataSource.checkEmail(email)

    override fun checkPassword(password: String, confirmPassword: String): Boolean =
        localAuthDataSource.checkPassword(password, confirmPassword)

    override suspend fun registerNewUser(email: String, password: String) =
        localAuthDataSource.registerNewUser(email, password)

    override fun logout() = localAuthDataSource.logout()

}