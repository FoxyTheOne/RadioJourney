package com.myproject.radiojourney.data.repository

import com.myproject.radiojourney.data.dataSource.local.auth.ILocalAuthDataSource
import com.myproject.radiojourney.domain.iRepository.IAuthRepository
import javax.inject.Inject

/**
 * Repository. Data layer. Работает с Local и Remote data source.
 * Подписка на локальную базу данных Room.
 * При работе с model, здесь происходит запрос в remote, преобразование remote -> local, сохранение результата в базу данных.
 */
class AuthRepository @Inject constructor(
    private val localAuthDataSource: ILocalAuthDataSource
) : IAuthRepository {
    override suspend fun isRememberLoginAndPasswordSelected(): Boolean =
        localAuthDataSource.isRememberLoginAndPasswordSelected()

    override suspend fun getEmail(): String? = localAuthDataSource.getEmail()
    override suspend fun getPassword(): String? = localAuthDataSource.getPassword()

    override suspend fun onLoginClicked(emailText: String, passwordText: String): Boolean =
        localAuthDataSource.onLoginClicked(emailText, passwordText)

    // Каждый раз, когда мы кликаем, будет исполняться этот метод. Здесь мы сохраняем статус check box
    override suspend fun setRememberLoginAndPasswordSelectedOrNot(isSelected: Boolean) =
        localAuthDataSource.setRememberLoginAndPasswordSelectedOrNot(isSelected)

    override suspend fun checkEmail(email: String): Boolean = localAuthDataSource.checkEmail(email)

    override suspend fun checkPassword(password: String, confirmPassword: String): Boolean =
        localAuthDataSource.checkPassword(password, confirmPassword)

    override suspend fun registerNewUser(email: String, password: String) =
        localAuthDataSource.registerNewUser(email, password)

    override fun logout() = localAuthDataSource.logout()
}