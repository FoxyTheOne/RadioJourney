package com.myproject.radiojourney.domain.logOut

import com.myproject.radiojourney.domain.iRepository.IAuthRepository
import javax.inject.Inject

class LogOutInteractor @Inject constructor(
    private val authRepository: IAuthRepository
) : ILogOutInteractor {

    override suspend fun onLogout() = authRepository.logout()

}