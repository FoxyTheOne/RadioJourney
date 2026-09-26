package com.myproject.radiojourney.domain.firstScreenLoadingUseCase

import kotlinx.coroutines.flow.Flow

/**
 * UseCase первого (приветственного) экрана: пройден ли он уже.
 *
 * Аккаунтов в приложении нет, названия "login" остались от удалённых экранов регистрации и входа (см. ILocalUserDataSource)
 */
interface ILoginScreenUseCase {
    // Пользователь нажал "Start journey"
    suspend fun onLoginClicked()

    // Первый экран уже пройден - открываем сразу карту. Flow, потому что значение читается из DataStore асинхронно
    fun isLoggedIn(): Flow<Boolean>
}