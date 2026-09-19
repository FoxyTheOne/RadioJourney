package com.myproject.radiojourney.presentation.content.base

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.myproject.radiojourney.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Базовый фрагмент для content фрагментов с toolbar.
 * Содержит меню toolbar с кнопкой выхода, диалог подтверждения и сам выход.
 * Раньше меню было скопировано в каждый фрагмент, а метод logout() - в каждую его ViewModel
 */
@AndroidEntryPoint
abstract class BaseContentFragmentAbstract : Fragment() {
    private val logOutViewModel by viewModels<LogOutViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ответ диалога выхода получаем через Fragment Result API (рекомендация developer.android.com).
        // Раньше фрагмент передавал себя диалогу через сеттер: после пересоздания (смена темы, языка) диалог
        // восстанавливался без слушателя, и кнопка "Да" ничего не делала
        childFragmentManager.setFragmentResultListener(LogOutDialogFragment.REQUEST_KEY, this) { _, _ ->
            logOutViewModel.logout()
            activity?.finish()
        }
    }

    // TOOLBAR - где будет находиться в нашем layout.
    // Раньше Activity передавалась фрагментам через Hilt как IAppSettings (приведение контекста к интерфейсу в ActivityModule)
    protected fun setToolbar(toolbar: Toolbar) {
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
    }

    // TOOLBAR: меню с кнопкой выхода. Вызывается из onViewCreated фрагмента.
    // MenuProvider привязан к viewLifecycleOwner, RESUMED - меню показывается только у открытого экрана
    protected fun setupToolbarMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.home_toolbar_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                if (menuItem.itemId == R.id.log_out) {
                    showLogoutDialog()
                    true
                } else {
                    false
                }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showLogoutDialog() {
        // Создаём LogOutDialogFragment для диалогового окна. childFragmentManager - ответ придёт именно этому фрагменту
        LogOutDialogFragment().show(childFragmentManager, "LogOutDialogFragment")
    }
}