package com.myproject.radiojourney.data.worker

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Запуск загрузки списка стран (CountryCacheWorker) и её прогресс для экрана.
 * Экран и ViewModel не работают с WorkManager напрямую - только через этот класс.
 */
@Singleton
class CountryCacheScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val UNIQUE_WORK_NAME = "CountryCache"
    }

    private val workManager get() = WorkManager.getInstance(context)

    // Запускается при каждом запуске приложения: список стран на сервере может измениться.
    // KEEP - если загрузка уже идёт (например, Activity пересоздалась), вторую не начинаем
    fun start() {
        val request = OneTimeWorkRequestBuilder<CountryCacheWorker>()
            // Без интернета WorkManager подождёт сеть, а не завершится ошибкой
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    // Прогресс загрузки 0..100 (100 - загрузка завершена)
    val progressLiveData: LiveData<Int> =
        workManager.getWorkInfosForUniqueWorkLiveData(UNIQUE_WORK_NAME).map { workInfos ->
            val workInfo = workInfos.lastOrNull() ?: return@map 0
            when (workInfo.state) {
                WorkInfo.State.SUCCEEDED -> 100
                else -> workInfo.progress.getInt(CountryCacheWorker.KEY_PROGRESS, 0)
            }
        }
}
