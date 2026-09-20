package com.myproject.radiojourney.data.repository

import com.myproject.radiojourney.data.dataSource.local.auth.ILocalUserDataSource
import com.myproject.radiojourney.domain.iRepository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Data layer, Repository. Работает с Local и Remote data source.
 *
 * Repository - объект, предоставляющий доступ к данным с возможностью выбора источника данных в зависимости от условий.
 * Подписка на локальную базу данных Room. Раскладываем данные.
 * При работе с model, здесь происходит запрос в remote, преобразование remote -> local, сохранение результата в базу данных.
 */
class AuthRepository @Inject constructor(
    private val localUserDataSource: ILocalUserDataSource
) : IAuthRepository {
    override suspend fun onLoginClicked() = localUserDataSource.onLoginClicked()

    override fun isLoggedIn(): Flow<Boolean> = localUserDataSource.isLoggedIn()

    override suspend fun logout() = localUserDataSource.logout()
}