package com.myproject.radiojourney.domain.logOutUseCase

interface ILogOutUseCase {
    // Не suspend: выход вызывается прямо перед закрытием Activity. В корутине viewModelScope он мог отмениться
    // вместе с ViewModel раньше, чем сохранится (Activity закрывается сразу после вызова)
    fun onLogout()
}