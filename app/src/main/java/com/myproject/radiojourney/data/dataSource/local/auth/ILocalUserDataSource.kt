package com.myproject.radiojourney.data.dataSource.local.auth

import kotlinx.coroutines.flow.Flow

/**
 * Local data source первого экрана: пройден ли он уже.
 *
 * Никакого аккаунта в приложении нет: раньше здесь были экраны регистрации и входа, от них остались название
 * ("login") и случайный токен в настройках. Сейчас это просто отметка "приветственный экран пройден",
 * по которой приложение решает, открывать его снова или сразу карту.
 *
 * Интерфейс лежит в data, потому что это деталь хранения. Экраны с ним не работают - они видят только репозиторий
 */
interface ILocalUserDataSource {
    // Пользователь нажал "Start journey" на первом экране
    suspend fun onLoginClicked()

    // Первый экран уже пройден - открываем сразу карту
    fun isLoggedIn(): Flow<Boolean>
}