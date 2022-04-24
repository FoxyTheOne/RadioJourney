package com.myproject.radiojourney.domain.firstScreenLoading

import com.myproject.radiojourney.domain.iRepository.IAuthRepository
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin. Работает только с Repository.
 *
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor).
 * При работе с model, здесь происходит преобразование local -> presentation, т.е.
 * преобразование моделей в модели нижнего уровня перед тем, как нижний уровень сможет их использовать.
 */
class LoginScreenUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) : ILoginScreenUseCase {
    override suspend fun onLoginClicked() = authRepository.onLoginClicked()
}