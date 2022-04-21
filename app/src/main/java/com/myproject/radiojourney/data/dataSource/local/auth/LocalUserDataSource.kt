package com.myproject.radiojourney.data.dataSource.local.auth

import com.myproject.radiojourney.data.localDatabaseRoom.IUserDAO
import com.myproject.radiojourney.data.sharedPreference.IAppSharedPreference
import com.myproject.radiojourney.utils.extension.isEmailValid
import com.myproject.radiojourney.utils.extension.isPasswordValid
import com.myproject.radiojourney.model.local.UserEntity
import com.myproject.radiojourney.model.local.UserWithStations
import javax.inject.Inject

/**
 * LocalAuthDataSource Будет доставать данные, либо сохранять их в локальную базу данных (SharedPreference, Room)
 */
class LocalUserDataSource @Inject constructor(
    private val preference: IAppSharedPreference,
    private val userDAO: IUserDAO
) : ILocalUserDataSource {
    override suspend fun getUsersWithStations(): List<UserWithStations> =
        userDAO.getUsersWithStations()

    override suspend fun isRememberLoginAndPasswordSelected(): Boolean =
        preference.isRememberLoginAndPasswordSelected()

    override suspend fun getEmail(): String = preference.getEmail()
    override suspend fun getPassword(): String = preference.getPassword()

    // Проверка, есть ли такой зарегистрированный User и верно ли введен пароль
    override suspend fun onLoginClicked() {
        val token = Math.random()*1000
        preference.saveToken(token.toInt())
    }

    // Каждый раз, когда мы кликаем, будет исполняться этот метод. Здесь мы сохраняем статус check box
    override suspend fun setRememberLoginAndPasswordSelectedOrNot(isSelected: Boolean) {
        preference.setRememberLoginAndPasswordSelectedOrNot(isSelected)
    }

    // Проверка логина, пароля. Регистрация нового пользователя
    override suspend fun checkEmail(email: String): Boolean {
        // Верно ли введён email
        val isEmailValid = email.isEmailValid()
        // Свободен ли email (возможно, уже зарегистрирован)
        val lookForSuchUserRegisteredAlready = userDAO.getUser(email)

        return isEmailValid && lookForSuchUserRegisteredAlready == null
    }

    override suspend fun checkPassword(password: String, confirmPassword: String): Boolean {
        // Верно ли введён пароль. Подтверждение пароля можно не проверять, т.к. оно должно быть точно таким же
        val isPasswordValid = password.isPasswordValid()
        // Совпадают ли введённые пароли
        val isPasswordEqualConfirmPassword = password == confirmPassword

        return isPasswordValid && isPasswordEqualConfirmPassword
    }

    override suspend fun registerNewUser(email: String, password: String) {
        userDAO.saveUser(UserEntity(email = email, password = password))
    }

    // Выход из аккаунта
    override fun logout() = preference.saveToken(null)
}