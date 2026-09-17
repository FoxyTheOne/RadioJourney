package com.myproject.radiojourney

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

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