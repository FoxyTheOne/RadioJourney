package com.myproject.radiojourney.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Хранилище пар ключ-значение на DataStore (рекомендация developer.android.com вместо SharedPreferences).
 *
 * Отличия от SharedPreferences, ради которых сделана замена:
 * - чтение не блокирует главный поток: DataStore читает файл в фоновом потоке и отдаёт значения через Flow/suspend.
 *   У SharedPreferences первое обращение читает файл целиком прямо в вызывающем потоке (обычно в главном);
 * - запись либо проходит целиком, либо не проходит совсем (файл пишется через временный и переименовывается),
 *   поэтому нет повреждённых настроек после выключения телефона в неподходящий момент;
 * - об изменении значения подписчик узнаёт сам (Flow), не нужен OnSharedPreferenceChangeListener;
 * - ошибку чтения файла видно (исключение в Flow), а не "тихое" значение по умолчанию.
 *
 * Старые настройки пользователей не теряются: SharedPreferencesMigration при первом обращении переносит
 * значения из файла AppSharedPreference и удаляет его
 */
@Singleton
class AppPreferenceStorage @Inject constructor(
    @ApplicationContext private val context: Context
) : IAppPreferenceStorage {

    companion object {
        private const val DATA_STORE_NAME = "app_preferences"
        private const val OLD_SHARED_PREFERENCES_NAME = "AppSharedPreference"

        // Ключи именованные и типизированные: положить в TOKEN строку, а прочитать число не получится (в отличие от SharedPreferences)
        private val TOKEN = stringPreferencesKey("USER_TOKEN")
        private val IS_FIRST_START = booleanPreferencesKey("PREFERENCE_IS_FIRST_START")
        private val LAST_LISTENED_URL = stringPreferencesKey("PREFERENCE_LAST_LISTENED_URL")
        private val LAST_COUNTRY_CODE = stringPreferencesKey("PREFERENCE_LAST_COUNTRY_CODE")
        private val IS_HIDE_INFO_CLICKED = booleanPreferencesKey("PREFERENCE_IS_HIDE_INFO_CLICKED")

        // Делегат создаёт единственный экземпляр DataStore на файл. Два экземпляра на один файл - ошибка времени выполнения,
        // поэтому делегат объявляют на верхнем уровне файла, а класс с ним работает через @Singleton
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = DATA_STORE_NAME,
            // Перенос значений из старого файла SharedPreferences (делается один раз, при первом чтении)
            produceMigrations = { appContext ->
                listOf(
                    SharedPreferencesMigration(
                        appContext,
                        OLD_SHARED_PREFERENCES_NAME
                    )
                )
            }
        )
    }

    private val dataStore get() = context.dataStore

    override val isLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
        val token = preferences[TOKEN].orEmpty()
        val isFirstStart = preferences[IS_FIRST_START] ?: true
        token.isNotBlank() && !isFirstStart
    }

    override suspend fun saveToken(token: Int?) {
        dataStore.edit { preferences -> preferences[TOKEN] = token?.toString().orEmpty() }
    }

    override suspend fun setIsFirstStart(isFirstStart: Boolean) {
        dataStore.edit { preferences -> preferences[IS_FIRST_START] = isFirstStart }
    }

    override suspend fun saveLastUsedRadioStationUrl(urlResolved: String) {
        dataStore.edit { preferences -> preferences[LAST_LISTENED_URL] = urlResolved }
    }

    // first() берёт текущее значение и отписывается. Flow здесь не нужен: адрес станции читается один раз, при запуске сервиса
    override suspend fun getLastUsedRadioStationUrl(): String =
        dataStore.data.first()[LAST_LISTENED_URL].orEmpty()

    override suspend fun saveLastUsedRadioStationCountryCode(countryCode: String) {
        dataStore.edit { preferences -> preferences[LAST_COUNTRY_CODE] = countryCode }
    }

    override suspend fun getLastUsedRadioStationCountryCode(): String =
        dataStore.data.first()[LAST_COUNTRY_CODE].orEmpty()

    override val isHideInfoClicked: Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[IS_HIDE_INFO_CLICKED] ?: false }

    override suspend fun setIsHideInfoClicked(isHideInfoClicked: Boolean) {
        dataStore.edit { preferences -> preferences[IS_HIDE_INFO_CLICKED] = isHideInfoClicked }
    }
}