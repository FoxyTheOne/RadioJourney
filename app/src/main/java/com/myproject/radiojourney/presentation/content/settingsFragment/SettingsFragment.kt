package com.myproject.radiojourney.presentation.content.settingsFragment

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.myproject.radiojourney.IAppSettings
import com.myproject.radiojourney.R
import com.myproject.radiojourney.databinding.LayoutSettingsBinding
import com.myproject.radiojourney.presentation.content.base.BaseContentFragmentAbstract
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.content.Intent
import android.net.Uri
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle

/**
 * Страница настроек.
 */
@AndroidEntryPoint
class SettingsFragment : BaseContentFragmentAbstract() {
    companion object {
        private const val TAG = "SettingsFragment"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: LayoutSettingsBinding? = null

    @Inject
    lateinit var appSettings: IAppSettings

    private val viewModel by viewModels<SettingsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = LayoutSettingsBinding.inflate(inflater, container, false)
        // TOOLBAR
//        setHasOptionsMenu(true) // setHasOptionsMenu deprecated
        // TOOLBAR - где будет находиться в нашем layout
        binding?.let {
            appSettings.setToolbar(it.homeToolbar)
        }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TOOLBAR in TIRAMISU
        // The usage of an interface lets you inject your own implementation
        val menuHost: MenuHost = requireActivity()

        // Add menu items without using the Fragment Menu APIs
        // Note how we can tie the MenuProvider to the viewLifecycleOwner
        // and an optional Lifecycle.State (here, RESUMED) to indicate when
        // the menu should be visible
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.home_toolbar_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.log_out -> {
                        showLogoutDialog()
                        Log.d(TAG, "showLogoutDialog() was called")
                        true
                    }

                    else -> {
                        // If we got here, the user's action was not recognized.
                        Log.d(TAG, "else result")
                        false
                    }
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        initListeners()
        subscribeOnLiveData()
    }

    private fun initListeners() {
        binding?.imageArrowBack?.setOnClickListener {
            if (this.findNavController().currentDestination?.id == R.id.settingsFragment) {
                this.findNavController()
                    .navigate(R.id.action_settingsFragment_to_homeRadioFragment)
            }
        }
        binding?.mail?.setOnClickListener {
            val subject = "RadioJourney app"
            val message = "Input your message"
            val email = "gartel.av@gmail.com"

            val selectorIntent = Intent(Intent.ACTION_SENDTO)
            selectorIntent.data = Uri.parse("mailto:") // only email apps should handle this

            val emailIntent = Intent(Intent.ACTION_SEND)
            emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
            emailIntent.putExtra(Intent.EXTRA_TEXT, message)
            emailIntent.selector = selectorIntent

            requireActivity().startActivity(
                Intent.createChooser(
                    emailIntent,
                    "Choose an Email client :"
                )
            )
        }
    }

    private fun subscribeOnLiveData() {
        // Показываем или прячем Progress
        viewModel.showProgressLiveData.observe(viewLifecycleOwner) {
            showProgress()
        }
        viewModel.hideProgressLiveData.observe(viewLifecycleOwner) {
            hideProgress()
        }
    }

    private fun showProgress() {
        binding?.frameLayout?.isVisible = true
        binding?.progressCircular?.isVisible = true
    }

    private fun hideProgress() {
        binding?.frameLayout?.isVisible = false
        binding?.progressCircular?.isVisible = false
    }

//    // TOOLBAR
//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        super.onCreateOptionsMenu(menu, inflater)
//        inflater.inflate(R.menu.home_toolbar_menu, menu)
//    }
//
//    // TOOLBAR - обработка клика
//    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
//        R.id.log_out -> {
//            showLogoutDialog()
//            Log.d(TAG, "showLogoutDialog() was called")
//            true
//        }
//        else -> {
//            // If we got here, the user's action was not recognized.
//            // Invoke the superclass to handle it.
//            Log.d(TAG, "else result")
//            super.onOptionsItemSelected(item)
//        }
//    }

    // TOOLBAR - Описываем метод из интерфейса ILogOutListener для выхода из аккаунта приложения
    override fun onLogOut() {
        viewModel.logout()
        activity?.finish()
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}