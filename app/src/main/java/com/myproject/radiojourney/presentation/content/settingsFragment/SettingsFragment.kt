package com.myproject.radiojourney.presentation.content.settingsFragment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.myproject.radiojourney.R
import com.myproject.radiojourney.databinding.LayoutSettingsBinding
import com.myproject.radiojourney.presentation.common.popBackStackSafely
import dagger.hilt.android.AndroidEntryPoint

/**
 * Экран "О программе" (кнопка "i" на карте): о приложении, откуда берутся станции,
 * ссылки на сайт проекта и политику конфиденциальности, письмо автору.
 *
 * Настроек здесь пока нет, отсюда и название файла: экран остался от задуманных настроек
 */
@AndroidEntryPoint
class SettingsFragment : Fragment() {
    companion object {
        private const val SITE_URL = "https://foxytheone.github.io/"

        // Политику конфиденциальности требует Google Play. Страница лежит в этом же репозитории (docs/privacy-policy.html)
        // и публикуется через GitHub Pages - так она всегда совпадает с тем, что делает приложение
        private const val PRIVACY_POLICY_URL =
            "https://foxytheone.github.io/RadioJourney/privacy-policy.html"
    }

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
        binding?.linearSite?.setOnClickListener { openLink(SITE_URL) }
        binding?.linearPrivacy?.setOnClickListener { openLink(PRIVACY_POLICY_URL) }
        binding?.mail?.setOnClickListener {
            val subject = "RadioJourney app"
            val message = getString(R.string.settings_mailMessage)
            val email = getString(R.string.settings_mail)

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
                    getString(R.string.settings_mailChooser)
                )
            )
        }
    }

    // Открыть страницу в браузере. Если браузера на телефоне нет (бывает на "чистых" прошивках),
    // startActivity бросил бы ActivityNotFoundException и приложение упало бы - поэтому ловим её и показываем сообщение
    private fun openLink(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(
                requireView(),
                getString(R.string.settings_noBrowser),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}