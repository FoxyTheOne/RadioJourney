package com.myproject.radiojourney.presentation.content.settingsFragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import com.myproject.radiojourney.R
import com.myproject.radiojourney.databinding.LayoutSettingsBinding
import com.myproject.radiojourney.presentation.content.base.BaseContentFragmentAbstract
import dagger.hilt.android.AndroidEntryPoint

/**
 * Страница настроек.
 */
@AndroidEntryPoint
class SettingsFragment : BaseContentFragmentAbstract() {
    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: LayoutSettingsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = LayoutSettingsBinding.inflate(inflater, container, false)
        // TOOLBAR
        binding?.let { setToolbar(it.homeToolbar) }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbarMenu()
        initListeners()
    }

    private fun initListeners() {
        binding?.imageArrowBack?.setOnClickListener {
            if (this.findNavController().currentDestination?.id == R.id.settingsFragment) {
                this.findNavController()
                    .navigate(R.id.action_settingsFragment_to_homeRadioFragment)
            }
        }
        binding?.linearForCoffee?.setOnClickListener {
            val browserIntent =
                Intent(Intent.ACTION_VIEW, "https://boosty.to/foxynest/donate".toUri())
            startActivity(browserIntent)
        }
        binding?.mail?.setOnClickListener {
            val subject = "RadioJourney app"
            val message = "Input your message"
            val email = "gartel.av@gmail.com"

            val selectorIntent = Intent(Intent.ACTION_SENDTO)
            selectorIntent.data = "mailto:".toUri() // only email apps should handle this

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

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}