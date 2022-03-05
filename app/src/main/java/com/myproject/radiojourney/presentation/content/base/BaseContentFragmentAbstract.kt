package com.myproject.radiojourney.presentation.content.base

import androidx.fragment.app.Fragment

/**
 * Здесь разместим методы, которые должны быть доступны для всех фрагментов с контентом (после успешной аутентификации)
 */
abstract class BaseContentFragmentAbstract: Fragment(), ILogOutListener {
    fun showLogoutDialog() {
        val supportFragment = requireActivity().supportFragmentManager
        // Создаём DialogFragment для диалогового окна
        val logOutDialogFragment = LogOutDialogFragment()
        logOutDialogFragment.setLogOutListener(this)
        logOutDialogFragment.show(supportFragment, "LogOutDialogFragment")
    }
}