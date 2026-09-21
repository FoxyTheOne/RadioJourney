package com.myproject.radiojourney.domain.iRepository

import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий входа. Интерфейс объявлен в domain, а реализован в data - так domain ничего не знает
 * ни про DataStore, ни про Room, ни про сеть (правило чистой архитектуры: зависимости смотрят внутрь, к domain)
 */
interface IAuthRepository {
    suspend fun onLoginClicked()
    fun isLoggedIn(): Flow<Boolean>
}