package com.myproject.radiojourney.data.dataSource.local.auth

import com.myproject.radiojourney.data.sharedPreference.IAppSharedPreference
import javax.inject.Inject

/**
 * LocalAuthDataSource Будет доставать данные, либо сохранять их в локальную базу данных (SharedPreference, Room)
 */
class LocalUserDataSource @Inject constructor(
    private val preference: IAppSharedPreference
) : ILocalUserDataSource {
    // Сохраняем токен, чтобы пользователь мог миновать первый экран, если уже выполнил все его условия
    override suspend fun onLoginClicked() {
        val token = Math.random() * 1000
        preference.saveToken(token.toInt())

        preference.setIsFirstStart(isFirstStart = false)
    }

    override fun isLoggedIn(): Boolean =
        preference.getToken().isNotBlank() && !preference.isFirstStart()

    // Выход из аккаунта
    override fun logout() {
        preference.saveToken(null)

        preference.setIsFirstStart(isFirstStart = true)
    }
}