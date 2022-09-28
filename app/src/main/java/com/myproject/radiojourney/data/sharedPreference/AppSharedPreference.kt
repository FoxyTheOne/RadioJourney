package com.myproject.radiojourney.data.sharedPreference

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppSharedPreference @Inject constructor(@ApplicationContext context: Context) :
    IAppSharedPreference {
    companion object {
        private const val TAG = "AppSharedPreference"

        // Константа - имя файла
        private const val PREFERENCE_NAME = "AppSharedPreference"

        // Ключ для отслеживания, проставлена галочка или нет
        private const val PREFERENCE_IS_REMEMBER_LOGIN_AND_PASSWORD_SELECTED =
            "PREFERENCE_IS_REMEMBER_LOGIN_AND_PASSWORD_SELECTED"
        private const val PREFERENCE_IS_RADIO_STATION_STORED =
            "PREFERENCE_IS_RADIO_STATION_STORED"

        // Ключи для данных
        private const val PREFERENCE_USER_EMAIL = "PREFERENCE_EMAIL"
        private const val PREFERENCE_USER_PASSWORD = "PREFERENCE_PASSWORD"
        private const val PREFERENCE_USER_TOKEN = "USER_TOKEN"
        private const val PREFERENCE_RADIO_STATION_URL = "PREFERENCE_RADIO_STATION_URL"
        private const val PREFERENCE_LAST_LISTENED_URL = "PREFERENCE_LAST_LISTENED_URL"
        private const val PREFERENCE_LAST_COUNTRY_CODE = "PREFERENCE_LAST_COUNTRY_CODE"
    }

    // У нас будет один общий файл, поэтому .getSharedPreferences()
    private val sharedPreference = context.getSharedPreferences(
        PREFERENCE_NAME,
        Context.MODE_PRIVATE
    )

    // Каждый раз, когда пользователь кликает по галочке, сюда передаётся значение
    override fun setRememberLoginAndPasswordSelectedOrNot(isSelected: Boolean) {
        sharedPreference?.edit()?.putBoolean(
            PREFERENCE_IS_REMEMBER_LOGIN_AND_PASSWORD_SELECTED,
            isSelected
        )?.apply()
        Log.d(TAG, "Сохранено в AppSharedPreference: CHECK_BOX_SELECTED = $isSelected")
    }

    // Выбрана ли галочка "оставаться в системе", т.е. сохранить данные (выбирали ли эту галочку ранее)
    // edit() нам уже не нужен, т.к. мы не будем ничего изменять
    // false в нашем случае - это дефолтное значение. Т.е. если ранее ничего не выбрано, возвратится false
    override fun isRememberLoginAndPasswordSelected(): Boolean {
        return sharedPreference?.getBoolean(
            PREFERENCE_IS_REMEMBER_LOGIN_AND_PASSWORD_SELECTED,
            false
        ) ?: false
    }

    override fun saveEmail(email: String) {
        sharedPreference?.edit()?.putString(
            PREFERENCE_USER_EMAIL,
            email
        )?.apply()
    }

    override fun getEmail(): String {
        return sharedPreference?.getString(
            PREFERENCE_USER_EMAIL,
            ""
        ) ?: ""
    }

    override fun savePassword(password: String) {
        sharedPreference?.edit()?.putString(
            PREFERENCE_USER_PASSWORD,
            password
        )?.apply()
    }

    override fun getPassword(): String {
        return sharedPreference?.getString(
            PREFERENCE_USER_PASSWORD,
            ""
        ) ?: ""
    }

    @Synchronized
    override fun saveToken(token: Int?) {
        sharedPreference?.edit()?.putString(
            PREFERENCE_USER_TOKEN,
            (token ?: "").toString()
        )?.apply()
    }

    @Synchronized
    override fun getToken(): String {
        return sharedPreference?.getString(
            PREFERENCE_USER_TOKEN,
            ""
        ) ?: ""
    }

    override fun setIsRadioStationStored(isStored: Boolean) {
        sharedPreference?.edit()?.putBoolean(
            PREFERENCE_IS_RADIO_STATION_STORED,
            isStored
        )?.apply()
        Log.d(
            TAG,
            "Сохранено в AppSharedPreference: PREFERENCE_IS_RADIO_STATION_STORED = $isStored"
        )
    }

    override fun isRadioStationStored(): Boolean {
        return sharedPreference?.getBoolean(
            PREFERENCE_IS_RADIO_STATION_STORED,
            false
        ) ?: false
    }

    override fun saveRadioStationUrl(urlResolved: String) {
        sharedPreference?.edit()?.putString(
            PREFERENCE_RADIO_STATION_URL,
            urlResolved
        )?.apply()
    }

    override fun getRadioStationUrl(): String {
        return sharedPreference?.getString(
            PREFERENCE_RADIO_STATION_URL,
            ""
        ) ?: ""
    }

    override fun saveLastUsedRadioStationUrl(urlResolved: String) {
        sharedPreference?.edit()?.putString(
            PREFERENCE_LAST_LISTENED_URL,
            urlResolved
        )?.apply()
    }

    override fun getLastUsedRadioStationUrl(): String {
        return sharedPreference?.getString(
            PREFERENCE_LAST_LISTENED_URL,
            ""
        ) ?: ""
    }

    override fun saveLastUsedRadioStationCountryCode(countryCode: String) {
        sharedPreference?.edit()?.putString(
            PREFERENCE_LAST_COUNTRY_CODE,
            countryCode
        )?.apply()
    }

    override fun getLastUsedRadioStationCountryCode(): String {
        return sharedPreference?.getString(
            PREFERENCE_LAST_COUNTRY_CODE,
            ""
        ) ?: ""
    }
}