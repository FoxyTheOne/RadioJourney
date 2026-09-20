package com.myproject.radiojourney.domain.logOutUseCase

import com.myproject.radiojourney.di.ApplicationScope
import com.myproject.radiojourney.domain.iRepository.IAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin. Работает только с Repository.
 *
 * Выход из аккаунта запускается в scope приложения, а не экрана: сразу после выхода экран закрывается,
 * и корутина viewModelScope успела бы отмениться раньше, чем запись дойдёт до хранилища
 */
class LogOutUseCase @Inject constructor(
    private val authRepository: IAuthRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ILogOutUseCase {
    override fun onLogout() {
        applicationScope.launch { authRepository.logout() }
    }
}