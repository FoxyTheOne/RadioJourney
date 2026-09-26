package com.myproject.radiojourney.data.dataSource.local.auth

import com.myproject.radiojourney.data.preference.IAppPreferenceStorage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Local data source первого экрана: читает и пишет отметку "экран пройден" в настройках (DataStore).
 *
 * Токен - наследство от удалённых экранов регистрации и входа: сейчас это просто случайное число,
 * по наличию которого приложение понимает, что первый экран уже был пройден
 */
class LocalUserDataSource @Inject constructor(
    private val preferenceStorage: IAppPreferenceStorage
) : ILocalUserDataSource {
    // Сохраняем токен, чтобы пользователь мог миновать первый экран, если уже выполнил все его условия
    override suspend fun onLoginClicked() {
        val token = Math.random() * 1000
        preferenceStorage.saveToken(token.toInt())

        preferenceStorage.setIsFirstStart(isFirstStart = false)
    }

    override fun isLoggedIn(): Flow<Boolean> = preferenceStorage.isLoggedIn
}