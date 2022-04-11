package com.myproject.radiojourney.presentation.content.base

import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * Базовый фрагмент для расширения content фрагментами.
 * Содержит логику открытия всплывающего окна при нажатии на кнопку выхода на тулбаре. Непосредственно логика метода onLogOut() описывается в фрагменте, который содержит тулбар
 */
@AndroidEntryPoint
abstract class BaseContentFragmentAbstract : Fragment(), ILogOutListener {
    fun showLogoutDialog() {
        val supportFragment = requireActivity().supportFragmentManager
        // Создаём LogOutDialogFragment для диалогового окна
        val logOutDialogFragment = LogOutDialogFragment()
        logOutDialogFragment.setLogOutListener(this)
        logOutDialogFragment.show(supportFragment, "LogOutDialogFragment")
    }
}