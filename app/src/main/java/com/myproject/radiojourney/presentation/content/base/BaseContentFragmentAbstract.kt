package com.myproject.radiojourney.presentation.content.base

import android.os.Bundle
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * Базовый фрагмент для расширения content фрагментами.
 * Содержит логику открытия всплывающего окна при нажатии на кнопку выхода на toolbar. Непосредственно логика метода onLogOut() описывается в фрагменте, который содержит toolbar
 */
@AndroidEntryPoint
abstract class BaseContentFragmentAbstract : Fragment(), ILogOutListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ответ диалога выхода получаем через Fragment Result API (рекомендация developer.android.com).
        // Раньше фрагмент передавал себя диалогу через сеттер: после пересоздания (смена темы, языка) диалог
        // восстанавливался без слушателя, и кнопка "Да" ничего не делала
        childFragmentManager.setFragmentResultListener(LogOutDialogFragment.REQUEST_KEY, this) { _, _ ->
            onLogOut()
        }
    }

    fun showLogoutDialog() {
        // Создаём LogOutDialogFragment для диалогового окна. childFragmentManager - ответ придёт именно этому фрагменту
        LogOutDialogFragment().show(childFragmentManager, "LogOutDialogFragment")
    }
}