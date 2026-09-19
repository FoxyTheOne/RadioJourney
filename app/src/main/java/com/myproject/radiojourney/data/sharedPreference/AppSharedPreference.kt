package com.myproject.radiojourney.data.sharedPreference

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Хранение небольших пар ключ-значение.
 * Удалены неиспользуемые методы, оставшиеся от удалённых экранов входа и регистрации (e-mail, пароль в открытом виде,
 * "запомнить меня") и от старого сохранения url станции
 */
class AppSharedPreference @Inject constructor(@ApplicationContext context: Context) :
    IAppSharedPreference {
    companion object {
        // Константа - имя файла
        private const val PREFERENCE_NAME = "AppSharedPreference"

        // Ключи для данных
        private const val PREFERENCE_USER_TOKEN = "USER_TOKEN"
        private const val PREFERENCE_IS_FIRST_START = "PREFERENCE_IS_FIRST_START"
        private const val PREFERENCE_LAST_LISTENED_URL = "PREFERENCE_LAST_LISTENED_URL"
        private const val PREFERENCE_LAST_COUNTRY_CODE = "PREFERENCE_LAST_COUNTRY_CODE"
        private const val PREFERENCE_IS_HIDE_INFO_CLICKED = "PREFERENCE_IS_HIDE_INFO_CLICKED"
    }

    // У нас будет один общий файл, поэтому .getSharedPreferences()
    // getSharedPreferences никогда не возвращает null, поэтому проверки "?." не нужны
    private val sharedPreference = context.getSharedPreferences(
        PREFERENCE_NAME,
        Context.MODE_PRIVATE
    )

    override fun saveToken(token: Int?) =
        sharedPreference.edit { putString(PREFERENCE_USER_TOKEN, (token ?: "").toString()) }

    override fun getToken(): String =
        sharedPreference.getString(PREFERENCE_USER_TOKEN, "") ?: ""

    override fun setIsFirstStart(isFirstStart: Boolean) =
        sharedPreference.edit { putBoolean(PREFERENCE_IS_FIRST_START, isFirstStart) }

    override fun isFirstStart(): Boolean =
        sharedPreference.getBoolean(PREFERENCE_IS_FIRST_START, true)

    override fun saveLastUsedRadioStationUrl(urlResolved: String) =
        sharedPreference.edit { putString(PREFERENCE_LAST_LISTENED_URL, urlResolved) }

    override fun getLastUsedRadioStationUrl(): String =
        sharedPreference.getString(PREFERENCE_LAST_LISTENED_URL, "") ?: ""

    override fun saveLastUsedRadioStationCountryCode(countryCode: String) =
        sharedPreference.edit { putString(PREFERENCE_LAST_COUNTRY_CODE, countryCode) }

    override fun getLastUsedRadioStationCountryCode(): String =
        sharedPreference.getString(PREFERENCE_LAST_COUNTRY_CODE, "") ?: ""

    override fun setIsHideInfoClicked(isHideInfoClicked: Boolean) =
        sharedPreference.edit { putBoolean(PREFERENCE_IS_HIDE_INFO_CLICKED, isHideInfoClicked) }

    override fun isHideInfoClicked(): Boolean =
        sharedPreference.getBoolean(PREFERENCE_IS_HIDE_INFO_CLICKED, false)
}