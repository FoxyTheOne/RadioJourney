package com.myproject.radiojourney

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Класс приложения: точка входа Hilt (@HiltAndroidApp) и настройка WorkManager.
 *
 * Configuration.Provider нужен, чтобы WorkManager умел создавать Worker'ы с @Inject-зависимостями (hilt-work).
 * Важно: в манифесте при этом отключается автоматическая инициализация WorkManager (InitializationProvider, tools:node="remove"),
 * иначе он запустится раньше Hilt и работать с @HiltWorker не сможет.
 *
 * Переиспользование: этот класс целиком подходит любому проекту с Hilt + WorkManager
 */
@HiltAndroidApp
class App : Application(), Configuration.Provider {

    // Фабрика от Hilt: без неё WorkManager не сможет передать зависимости (@Inject) в CountryCacheWorker.
    // Автоматическая инициализация WorkManager для этого отключена в AndroidManifest
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}