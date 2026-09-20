package com.myproject.radiojourney.data.dataSource.local.country

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Координаты центров стран для маркеров на карте.
 *
 * Раньше это была таблица в коде ProgressForegroundService (countyCodeAndLatLng), составленная вручную.
 * Такие справочные данные обычно хранят не в коде, а в файле ресурсов приложения: assets/country_coordinates.csv.
 * Так их проще обновлять и проверять, а код не растягивается на сотни строк.
 *
 * Для страны, которой нет в файле (сервер radio-browser может прислать новый код), координаты ищет системный Geocoder
 * по названию страны. Если и он не нашёл (нет сервиса геокодинга на устройстве, нет сети, код не является страной, например "XX"),
 * возвращается null, и маркер для этой страны не ставится. Раньше такие страны ставились в точку (0, 0) - в океан у Африки.
 */
@Singleton
class CountryCoordinatesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "CountryCoordinates"
        private const val ASSET_FILE = "country_coordinates.csv"
    }

    // Файл читается один раз, при первом обращении
    private val coordinatesFromAsset: Map<String, Pair<Double, Double>> by lazy { readAsset() }

    /** Широта и долгота центра страны или null, если найти не удалось */
    suspend fun getCoordinates(countryCode: String, countryName: String): Pair<Double, Double>? =
        coordinatesFromAsset[countryCode.uppercase()] ?: findWithGeocoder(countryCode, countryName)

    private fun readAsset(): Map<String, Pair<Double, Double>> =
        context.assets.open(ASSET_FILE).bufferedReader().useLines { lines ->
            lines.filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("country_code") }
                .mapNotNull { line ->
                    val (code, latitude, longitude) = line.split(",").takeIf { it.size == 3 } ?: return@mapNotNull null
                    val lat = latitude.toDoubleOrNull() ?: return@mapNotNull null
                    val lng = longitude.toDoubleOrNull() ?: return@mapNotNull null
                    code.trim() to (lat to lng)
                }
                .toMap()
        }

    private suspend fun findWithGeocoder(countryCode: String, countryName: String): Pair<Double, Double>? {
        // Locale("", "XX").displayName возвращает сам код, если такой страны нет - искать бессмысленно
        if (countryName.equals(countryCode, ignoreCase = true) || !Geocoder.isPresent()) {
            Log.d(TAG, "Координаты не найдены: $countryCode ($countryName)")
            return null
        }

        val geocoder = Geocoder(context, Locale.ENGLISH)
        val address = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // С Android 13 - асинхронный вариант, результат приходит в слушатель
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(countryName, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            continuation.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            continuation.resume(null)
                        }
                    })
                }
            } else {
                // До Android 13 - блокирующий вызов, поэтому в Dispatchers.IO
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(countryName, 1)?.firstOrNull()
                }
            }
        } catch (e: IOException) {
            null // нет сети или сервис геокодинга недоступен
        } catch (e: IllegalArgumentException) {
            null
        }

        Log.d(TAG, "Geocoder: $countryCode ($countryName) -> ${address?.latitude}, ${address?.longitude}")
        return address?.let { it.latitude to it.longitude }
    }
}