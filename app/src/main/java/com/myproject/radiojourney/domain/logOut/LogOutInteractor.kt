package com.myproject.radiojourney.domain.logOut

import com.myproject.radiojourney.domain.iRepository.IAuthRepository
import javax.inject.Inject

/**
 * Interactor. Domain layer. Работает только с Repository.
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor)
 * При работе с model, здесь происходит преобразование local -> presentation (опционально)
 */
class LogOutInteractor @Inject constructor(
    private val authRepository: IAuthRepository
) : ILogOutInteractor {
    override suspend fun onLogout() = authRepository.logout()
}