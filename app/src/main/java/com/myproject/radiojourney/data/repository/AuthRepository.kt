package com.myproject.radiojourney.data.repository

import android.util.Log
import com.myproject.radiojourney.data.dataSource.local.auth.ILocalUserDataSource
import com.myproject.radiojourney.domain.iRepository.IAuthRepository
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
    companion object {
        private const val TAG = "AuthRepository"
    }

    override suspend fun onLoginClicked() = localUserDataSource.onLoginClicked()

    override suspend fun getToken(): Int? {
        // Узнаём userCreatorId
        // В нашем случае userCreatorId = token
        val userToken = localUserDataSource.getToken()
        val userTokenInt = userToken.toIntOrNull()
        Log.d(TAG, "Результат преобразования $userToken String в Int = $userTokenInt")

        return userTokenInt
    }

    override fun logout() = localUserDataSource.logout()
}