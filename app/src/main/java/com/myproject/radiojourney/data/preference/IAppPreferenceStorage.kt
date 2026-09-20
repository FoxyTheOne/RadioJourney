package com.myproject.radiojourney.data.preference

import kotlinx.coroutines.flow.Flow

/**
 * Хранение небольших пар ключ-значение (настройки, токен, последняя станция).
 *
 * Значения, за которыми экран следит постоянно, отдаются как Flow: DataStore сам пришлёт новое значение,
 * когда его кто-то запишет. Значения, которые нужны один раз, читаются suspend-методом
 */
interface IAppPreferenceStorage {
    // Пользователь уже входил (есть токен и это не первый запуск)
    val isLoggedIn: Flow<Boolean>

    suspend fun saveToken(token: Int?)
    suspend fun setIsFirstStart(isFirstStart: Boolean)

    suspend fun saveLastUsedRadioStationUrl(urlResolved: String)
    suspend fun getLastUsedRadioStationUrl(): String
    suspend fun saveLastUsedRadioStationCountryCode(countryCode: String)
    suspend fun getLastUsedRadioStationCountryCode(): String

    // Скрыт ли информационный блок на главном экране
    val isHideInfoClicked: Flow<Boolean>
    suspend fun setIsHideInfoClicked(isHideInfoClicked: Boolean)
}