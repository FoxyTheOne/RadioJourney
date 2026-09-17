package com.myproject.radiojourney.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.android.gms.maps.model.LatLng
import com.myproject.radiojourney.data.dataSource.local.country.CountryCoordinates
import com.myproject.radiojourney.data.dataSource.local.radio.ILocalRadioDataSource
import com.myproject.radiojourney.data.dataSource.network.INetworkRadioDataSource
import com.myproject.radiojourney.entities.local.CountryLocal
import com.myproject.radiojourney.entities.remote.CountryCodeRemote
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Locale

/**
 * Загрузка списка стран с сервера и сохранение в Room (маркеры на карте).
 *
 * Раньше это делал ProgressForegroundService: foreground-сервис с уведомлением, а прогресс передавался на первый экран бродкастами.
 * Для разовой фоновой загрузки данных developer.android.com рекомендует WorkManager: он сам следит за сетью
 * (загрузка начнётся, когда появится интернет), переживает закрытие приложения, а прогресс экран получает через WorkInfo.
 */
@HiltWorker
class CountryCacheWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val networkRadioDataSource: INetworkRadioDataSource,
    private val localRadioDataSource: ILocalRadioDataSource
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "CountryCacheWorker"

        // Ключ прогресса в WorkInfo.progress: от 0 до 100
        const val KEY_PROGRESS = "progress"
    }

    override suspend fun doWork(): Result {
        // Получаем список кодов стран из networkRadioDataSource
        val countryCodeRemoteList = networkRadioDataSource.getCountryCodeList()
        if (countryCodeRemoteList.isEmpty()) {
            Log.d(TAG, "countryCodeRemoteList size = 0. The server is down. Please, try again later")
            // Сервер недоступен. Первый экран через несколько секунд покажет диалог о проблеме с сервером
            return Result.failure()
        }

        // Коды стран могут прийти маленькими буквами: переводим в большие и суммируем количество станций по коду страны
        val mergedCountryCodeRemoteList = countryCodeRemoteList
            .groupBy({ it.name.uppercase() }, { it.stationcount })
            .map { (countryCode, stationCounts) -> CountryCodeRemote(countryCode, stationCounts.sum()) }

        val listSize = mergedCountryCodeRemoteList.size
        var percentCount = 10

        val countryLocalList = mergedCountryCodeRemoteList.mapIndexed { index, countryCodeRemote ->
            // Узнаем название страны
            val countryName = Locale("", countryCodeRemote.name).displayName

            // Ищем локацию в нашей коллекции
            val countryLocation = CountryCoordinates.byCountryCode[countryCodeRemote.name] ?: LatLng(0.0, 0.0)
            if (countryLocation.latitude == 0.0) {
                Log.d(TAG, "!! Адрес не найден, countryCode: ${countryCodeRemote.name}, countryName: $countryName")
            }

            // Прогресс для полосы на первом экране - каждые 10%
            val countryCount = index + 1
            if (countryCount == percentCount * listSize / 100) {
                percentCount += 10
                setProgress(workDataOf(KEY_PROGRESS to 100 * countryCount / listSize))
            }

            // remote -> local
            CountryLocal.fromRemoteToLocal(
                countryCodeRemote,
                countryName = countryName,
                countryLocation = countryLocation
            )
        }

        // Теперь сохраним наши страны в Room
        localRadioDataSource.saveCountryList(countryLocalList.toMutableList())
        Log.d(TAG, "Список стран сохранён в локальную базу данных: size = ${countryLocalList.size}")
        return Result.success()
    }
}