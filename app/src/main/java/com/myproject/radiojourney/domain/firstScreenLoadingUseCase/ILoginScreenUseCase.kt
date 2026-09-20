package com.myproject.radiojourney.domain.firstScreenLoadingUseCase

import kotlinx.coroutines.flow.Flow

/**
 * UseCase первого экрана: вход в приложение и признак того, что пользователь уже входил
 */
interface ILoginScreenUseCase {
    suspend fun onLoginClicked()

    // Пользователь уже входил - первый экран пропускаем. Flow, потому что значение читается из DataStore асинхронно
    fun isLoggedIn(): Flow<Boolean>
}