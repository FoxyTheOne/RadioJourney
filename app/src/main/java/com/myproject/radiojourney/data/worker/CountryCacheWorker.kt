package com.myproject.radiojourney.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.myproject.radiojourney.data.dataSource.local.country.CountryCoordinatesDataSource
import com.myproject.radiojourney.data.dataSource.local.radio.ILocalRadioDataSource
import com.myproject.radiojourney.data.dataSource.network.INetworkRadioDataSource
import com.myproject.radiojourney.data.mapper.toLocal
import com.myproject.radiojourney.domain.model.Country
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
    private val localRadioDataSource: ILocalRadioDataSource,
    private val countryCoordinatesDataSource: CountryCoordinatesDataSource
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "CountryCacheWorker"

        // Ключ прогресса в WorkInfo.progress: от 0 до 100
        const val KEY_PROGRESS = "progress"
    }

    override suspend fun doWork(): Result {
        // Получаем список стран из networkRadioDataSource (/json/countries: код, название, число станций)
        val countryRemoteList = networkRadioDataSource.getCountryList()
        if (countryRemoteList.isEmpty()) {
            Log.d(TAG, "countryRemoteList size = 0. The server is down. Please, try again later")
            // Сервер недоступен. Первый экран через несколько секунд покажет диалог о проблеме с сервером
            return Result.failure()
        }

        // Коды стран могут прийти маленькими буквами: переводим в большие и суммируем количество станций по коду страны.
        // Страны без кода пропускаем - поставить их на карту и запросить их станции всё равно нельзя
        val countriesByCode = countryRemoteList
            .filter { !it.countryCode.isNullOrBlank() }
            .groupBy { it.countryCode!!.uppercase() }

        val listSize = countriesByCode.size
        var percentCount = 10
        var countryCount = 0
        val countryList = mutableListOf<Country>()

        for ((countryCode, remoteCountries) in countriesByCode) {
            val stationCount = remoteCountries.sumOf { it.stationCount }

            // Название страны - на языке телефона. Если Android такого кода не знает, он возвращает сам код ("XK"),
            // и тогда берём английское название, которое прислал сервер: оно же поможет Geocoder найти координаты
            val localName = Locale("", countryCode).displayName
            val serverName =
                remoteCountries.firstNotNullOfOrNull { it.name?.takeIf(String::isNotBlank) }
            val countryName = if (localName.isBlank() || localName == countryCode) serverName
                ?: countryCode else localName

            // Координаты - из файла assets/country_coordinates.csv, для новых стран - через Geocoder.
            // Страна без координат на карту не попадает (раньше её маркер ставился в точку 0, 0)
            val coordinates = countryCoordinatesDataSource.getCoordinates(countryCode, countryName)
            if (coordinates != null) {
                countryList += Country(
                    countryCode = countryCode,
                    stationCount = stationCount,
                    countryName = countryName,
                    latitude = coordinates.first,
                    longitude = coordinates.second
                )
            }

            // Прогресс для полосы на первом экране - каждые 10%
            countryCount++
            if (countryCount == percentCount * listSize / 100) {
                percentCount += 10
                setProgress(workDataOf(KEY_PROGRESS to 100 * countryCount / listSize))
            }
        }

        // Теперь сохраним наши страны в Room (список целиком заменяет старый)
        localRadioDataSource.replaceCountryList(countryList.map { it.toLocal() })
        Log.d(
            TAG,
            "Список стран сохранён в локальную базу данных: size = ${countryList.size} из $listSize"
        )
        return Result.success()
    }
}