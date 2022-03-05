package com.myproject.radiojourney.presentation.content.homeRadio

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.myproject.radiojourney.IAppSettings
import com.myproject.radiojourney.R
import com.myproject.radiojourney.presentation.content.base.BaseContentFragmentAbstract
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Главная страница.
 * Содержит карту с метками, описание выбранной радиостанции и кнопки "добавить в избранное", "перейти в мой список".
 *
 * С View binding не работает обработка клика Toolbar?
 */
@AndroidEntryPoint
class HomeRadioFragment : BaseContentFragmentAbstract() {
    companion object {
            private const val TAG = "HomeRadioFragment"
        }

    @Inject
    lateinit var appSettings: IAppSettings

    private val viewModel by viewModels<HomeRadioViewModel>()
    private lateinit var frameLayout: FrameLayout
    private lateinit var progressCircular: ProgressBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // TOOLBAR
        setHasOptionsMenu(true)
        val view = inflater.inflate(R.layout.layout_home_radio, container, false)
        // TOOLBAR - где будет находиться в нашем layout
        appSettings.setToolbar(view?.findViewById(R.id.home_toolbar))
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Переменные для отображения прогресса
        frameLayout = view.findViewById(R.id.frameLayout)
        progressCircular = view.findViewById(R.id.progressCircular)

//        initListeners()
        subscribeOnLiveData()
    }

    // TOOLBAR
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.home_toolbar_menu, menu)
    }

    // TOOLBAR - обработка клика
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.log_out -> {
            showLogoutDialog()
            Log.d(TAG, "showLogoutDialog() was called")
            true
        }
        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            Log.d(TAG, "else result")
            super.onOptionsItemSelected(item)
        }
    }

    // TOOLBAR - Описываем метод из интерфейса ILogOutListener для выхода из аккаунта приложения
    override fun onLogOut() {
        viewModel.logout()
        this.findNavController().navigate(R.id.action_homeRadioFragment_to_auth_nav_graph)
    }

    private fun initListeners() {
        TODO("Not yet implemented")
    }

    private fun subscribeOnLiveData() {
        // Показываем или прячем Progress
        viewModel.showProgressLiveData.observe(viewLifecycleOwner, {
            showProgress()
        })
        viewModel.hideProgressLiveData.observe(viewLifecycleOwner, {
            hideProgress()
        })
    }

    private fun showProgress() {
        frameLayout.isVisible = true
        progressCircular.isVisible = true
    }

    private fun hideProgress() {
        frameLayout.isVisible = false
        progressCircular.isVisible = false
    }

}