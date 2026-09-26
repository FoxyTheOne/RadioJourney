package com.myproject.radiojourney.data.repository

import com.myproject.radiojourney.data.dataSource.local.auth.ILocalUserDataSource
import com.myproject.radiojourney.domain.iRepository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Data layer, Repository первого экрана: пройден ли он (см. ILocalUserDataSource).
 *
 * Repository - объект, который решает, откуда брать данные. Здесь источник один, настройки в DataStore,
 * поэтому класс совсем короткий: он нужен, чтобы domain не знал, где именно хранится эта отметка
 */
class AuthRepository @Inject constructor(
    private val localUserDataSource: ILocalUserDataSource
) : IAuthRepository {
    override suspend fun onLoginClicked() = localUserDataSource.onLoginClicked()

    override fun isLoggedIn(): Flow<Boolean> = localUserDataSource.isLoggedIn()
}