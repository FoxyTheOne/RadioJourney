package com.myproject.radiojourney.domain.logOutUseCase

/**
 * UseCase выхода из аккаунта
 */
interface ILogOutUseCase {
    // Не suspend: экран вызывает выход и сразу закрывается. Сама запись идёт в scope приложения (см. LogOutUseCase)
    fun onLogout()
}