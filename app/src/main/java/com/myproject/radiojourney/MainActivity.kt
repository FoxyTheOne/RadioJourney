package com.myproject.radiojourney

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import dagger.hilt.android.AndroidEntryPoint

/**
 * Описать логику приложения.
 *
 * В этом проекте используется шаблон проектирования архитектуры приложения MVVM. Используется подход Clean Architecture.
 * При создании проекта я старалась опираться на принципы проектирования SOLID (Single responsibility, open-closed, liskov substitution, interface segregation, dependency inversion)
 * В проекте используется dependency injection Hilt, т.к. это рекомендация google. А так же потому, что мне нравится его использовать.
 * Так же в проекте я использую navigation, что я считаю очень удобным.
 * В фрагментах с аунтентификацией я использую View binding.
 *
 * Так же в проекте я научилась использовать свой custom font, color style, button color (градиент).
 * Названия всех интерфейсов я начинаю с буквы I, чтобы было порще ориентироваться в коде проекта.
 *
 * Для хранения небольших пар ключ-значение (логин и пароль, токен) я использую Shared preferences.
 * Shared preferences д.б. быть Singleton, поэтому для верности в Module я пометила аннотацией @Singleton
 *
 * Для запросов в ViewModel я пользуюсь Coroutines.
 * В основном использую Dispatchers.IO (создаёт необходмое количество потоков, но минимум 64), т.к. он предназначен
 * для выполнения операций ввода-вывода (н-р, операции с файлами, сетевыми запросами, локальной базой данных).
 * А так же launch, когда нам нужно просто сделать вызов функции.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), IAppSettings {
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

    override fun setToolbar(toolbar: Toolbar?) {
        setSupportActionBar(toolbar)
    }

}