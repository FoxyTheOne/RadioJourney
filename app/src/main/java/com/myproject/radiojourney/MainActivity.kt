package com.myproject.radiojourney

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import dagger.hilt.android.AndroidEntryPoint

/**
 * Mvvm
 * Clean Architecture
 * SOLID
 * Hilt
 * Навигация - с помощью navigation
 * Custom font, color style,button color (градиент)
 *
 * View binding
 * В своём проекте я не использовала View binding чисто из своих эстетических соображений.
 * Мне нравится, как выглядит код, где мы пользуемся переменными для view, которые инициализировали самостоятельно. Без множества safe операторов "?."
 * Однако, для демонстрации освоения этого материала, я воспользовалась View binding в SignInFragment
 *
 * Shared preferences. Должны быть Singleton, поэтому для верности в Module пометила аннотацией @Singleton
 *
 * Coroutines
 *
 * Названия всех интерфейсов я начинаю с буквы I, чтобы было порще ориентироваться в коде проекта и не возникало путаницы
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onBackPressed() {
        val fragmentCount = supportFragmentManager.backStackEntryCount

        if (fragmentCount > 1) {
            super.onBackPressed()
        } else {
            finish()
        }
    }

    private fun clearBackStack() = supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
}