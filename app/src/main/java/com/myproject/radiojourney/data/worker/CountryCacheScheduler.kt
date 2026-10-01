package com.myproject.radiojourney.data.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
        workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    // Раньше экран узнавал о неудаче по таймеру: "7 секунд прошло, а стран всё нет - значит, сервер не работает".
    // Первая загрузка часто идёт дольше, и сообщение об ошибке мелькало, хотя всё было в порядке. Теперь экран
    // смотрит на настоящее состояние задачи в WorkManager
    val status: Flow<Status> =
        workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_WORK_NAME).map { workInfos ->
            when (workInfos.lastOrNull()?.state) {
                WorkInfo.State.SUCCEEDED -> Status.SUCCEEDED
                WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> Status.FAILED
                // ENQUEUED бывает и на долю секунды перед запуском - поэтому экран ждёт немного, прежде чем
                // сказать "нет интернета" (см. FirstScreenLoadingViewModel)
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> Status.WAITING_FOR_NETWORK
                WorkInfo.State.RUNNING, null -> Status.RUNNING
            }
        }.distinctUntilChanged()

    // Прогресс загрузки 0..100 (100 - загрузка завершена)
    val progress: Flow<Int> =
        workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_WORK_NAME).map { workInfos ->
            val workInfo = workInfos.lastOrNull() ?: return@map 0
            when (workInfo.state) {
                WorkInfo.State.SUCCEEDED -> 100
                else -> workInfo.progress.getInt(CountryCacheWorker.KEY_PROGRESS, 0)
            }
        }

    // Что сейчас с загрузкой - для экрана, который ждёт список стран
    enum class Status {
        RUNNING,             // идёт (или вот-вот начнётся)
        WAITING_FOR_NETWORK, // поставлена в очередь, но не начинается: WorkManager ждёт интернет (см. setConstraints в start)
        FAILED,              // сервер не дал список (CountryCacheWorker вернул Result.failure)
        SUCCEEDED
    }
}