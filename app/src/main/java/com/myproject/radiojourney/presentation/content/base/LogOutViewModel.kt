package com.myproject.radiojourney.presentation.content.base

import androidx.lifecycle.ViewModel
import com.myproject.radiojourney.domain.logOutUseCase.ILogOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Выход из аккаунта для всех экранов с toolbar (см. BaseContentFragmentAbstract).
 * Раньше одинаковый logout() был в HomeRadioViewModel, BaseRadioListViewModel, SettingsViewModel и RecommendedListViewModel
 */
@HiltViewModel
class LogOutViewModel @Inject constructor(
    private val logOutInteractor: ILogOutUseCase
) : ViewModel() {
    fun logout() = logOutInteractor.onLogout()
}
