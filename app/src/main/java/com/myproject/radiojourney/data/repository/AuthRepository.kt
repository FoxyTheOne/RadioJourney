package com.myproject.radiojourney.data.repository

import com.myproject.radiojourney.data.dataSource.local.auth.ILocalUserDataSource
import com.myproject.radiojourney.domain.iRepository.IAuthRepository
import javax.inject.Inject

/**
 * Repository. Data layer. Работает с Local и Remote data source.
 * Подписка на локальную базу данных Room.
 * При работе с model, здесь происходит запрос в remote, преобразование remote -> local, сохранение результата в базу данных.
 */
class AuthRepository @Inject constructor(
    private val localUserDataSource: ILocalUserDataSource
) : IAuthRepository {
    override suspend fun isRememberLoginAndPasswordSelected(): Boolean =
        localUserDataSource.isRememberLoginAndPasswordSelected()

    override suspend fun getEmail(): String? = localUserDataSource.getEmail()
    override suspend fun getPassword(): String? = localUserDataSource.getPassword()

    override suspend fun onLoginClicked() =
        localUserDataSource.onLoginClicked()

    // Каждый раз, когда мы кликаем, будет исполняться этот метод. Здесь мы сохраняем статус check box
    override suspend fun setRememberLoginAndPasswordSelectedOrNot(isSelected: Boolean) =
        localUserDataSource.setRememberLoginAndPasswordSelectedOrNot(isSelected)

    override suspend fun checkEmail(email: String): Boolean = localUserDataSource.checkEmail(email)

    override suspend fun checkPassword(password: String, confirmPassword: String): Boolean =
        localUserDataSource.checkPassword(password, confirmPassword)

    override suspend fun registerNewUser(email: String, password: String) =
        localUserDataSource.registerNewUser(email, password)

    override fun logout() = localUserDataSource.logout()
}