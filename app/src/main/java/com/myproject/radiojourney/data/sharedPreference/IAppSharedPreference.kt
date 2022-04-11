package com.myproject.radiojourney.data.sharedPreference

interface IAppSharedPreference {
    // Каждый раз, когда пользователь кликает по галочке, сюда передаётся значение
    fun setRememberLoginAndPasswordSelectedOrNot(isSelected: Boolean)

    // Выбрана ли галочка "оставаться в системе", т.е. сохранить данные
    fun isRememberLoginAndPasswordSelected(): Boolean

    // Методы для сохранения данных при успешном входе, если выбрана галочка
    fun saveEmail(email: String)
    fun getEmail(): String

    fun savePassword(password: String)
    fun getPassword(): String

    fun saveToken(token: Int?)
    fun getToken(): String

    fun setIsRadioStationStored(isStored: Boolean)
    fun isRadioStationStored(): Boolean
    fun saveRadioStationUrl(url: String)
    fun getRadioStationUrl(): String
}