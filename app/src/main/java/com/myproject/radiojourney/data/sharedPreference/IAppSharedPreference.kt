package com.myproject.radiojourney.data.sharedPreference

interface IAppSharedPreference {
    fun saveToken(token: Int?)
    fun getToken(): String

    fun setIsFirstStart(isFirstStart: Boolean)
    fun isFirstStart(): Boolean

    fun saveLastUsedRadioStationUrl(urlResolved: String)
    fun getLastUsedRadioStationUrl(): String
    fun saveLastUsedRadioStationCountryCode(countryCode: String)
    fun getLastUsedRadioStationCountryCode(): String

    fun setIsHideInfoClicked(isHideInfoClicked: Boolean)
    fun isHideInfoClicked(): Boolean
}