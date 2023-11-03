package com.myproject.radiojourney.presentation.firstScreen

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.myproject.radiojourney.R
import com.myproject.radiojourney.data.sharedPreference.IAppSharedPreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Базовый фрагмент для расширения фрагментами с аутентификацией.
 * Содержит логику перехода сразу на content, если в предыдущий раз пользователь залогинился
 */
@AndroidEntryPoint
abstract class BaseAuthFragmentAbstract : Fragment() {
    companion object {
        private const val TAG = "BaseAuthFragment"
    }

    // Воспользуемся global navigation actions, чотбы открывать сразу content, если есть token
    @Inject
    lateinit var preference: IAppSharedPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenForCheck = preference.getToken()
        Log.d(TAG, "Проверка tokenForCheck = $tokenForCheck")

        val isFirstStart = preference.isFirstStart()
        Log.d(TAG, "Проверка isFirstStart = $isFirstStart")

        if (tokenForCheck.isNotBlank() && !isFirstStart) {
//            Toast.makeText(
//                context,
//                "Welcome back!",
//                Toast.LENGTH_LONG
//            ).show()
            findNavController().navigate(R.id.action_global_home_radio_fragment)
        } else {
//            Toast.makeText(
//                context,
//                "Thanks for uploading!",
//                Toast.LENGTH_LONG
//            ).show()
        }
    }
}