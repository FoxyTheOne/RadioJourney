package com.myproject.radiojourney.presentation.model

import com.google.android.gms.maps.model.LatLng
import com.myproject.radiojourney.domain.model.Country
import java.util.Locale

/**
 * Страна для экрана: то же, что и domain-модель Country, но с LatLng из Google Maps.
 *
 * Зачем отдельная модель: LatLng - класс библиотеки карт. Если положить его в domain-модель,
 * слой бизнес-логики станет зависеть от Google Maps, и его нельзя будет проверить обычным unit-тестом
 */
// Страна для маркера на карте. LatLng (Google Maps) используется только в слое экрана
data class CountryPresentation(
    val countryCode: String,
    val stationCount: Int,
    val countryName: String,
    val countryLocation: LatLng
)

fun Country.toPresentation() = CountryPresentation(
    countryCode = countryCode,
    stationCount = stationCount,
    countryName = localizedCountryName(countryCode, savedName = countryName),
    countryLocation = LatLng(latitude, longitude)
)

// Название страны на языке приложения в момент показа. В базе оно лежит на том языке, который был при загрузке
// списка стран (CountryCacheWorker): переключил язык телефона - и метки на карте оставались на старом, пока список
// не скачается заново. Locale знает названия стран на всех языках системы, поэтому переводим по коду страны.
// Если код ему незнаком (Locale возвращает сам код), берём сохранённое название
private fun localizedCountryName(countryCode: String, savedName: String): String {
    val name = Locale("", countryCode).displayCountry
    return if (name.isBlank() || name.equals(countryCode, ignoreCase = true)) savedName else name
}