package com.myproject.radiojourney.domain.firstScreenLoadingUseCase

import com.myproject.radiojourney.domain.iRepository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Domain layer, UseCase первого экрана: пройден ли он и отметка о том, что пользователь его прошёл.
 *
 * UseCase - это то, что умеет делать приложение, описанное на чистом Kotlin: он работает только с репозиториями
 * (через их интерфейсы) и ничего не знает ни про экраны, ни про Android. Для каждого экрана - свой UseCase
 */
class LoginScreenUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) : ILoginScreenUseCase {
    override suspend fun onLoginClicked() = authRepository.onLoginClicked()
    override fun isLoggedIn(): Flow<Boolean> = authRepository.isLoggedIn()
}