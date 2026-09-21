package com.myproject.radiojourney.presentation.content.settingsFragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.myproject.radiojourney.databinding.LayoutSettingsBinding
import com.myproject.radiojourney.presentation.common.popBackStackSafely
import dagger.hilt.android.AndroidEntryPoint

/**
 * Страница настроек.
 */
@AndroidEntryPoint
class SettingsFragment : Fragment() {
    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: LayoutSettingsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = LayoutSettingsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initListeners()
    }

    private fun initListeners() {
        // Назад на карту - так же, как системная кнопка "Назад" (см. комментарий в app_navigation.xml)
        binding?.imageArrowBack?.setOnClickListener { popBackStackSafely() }
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