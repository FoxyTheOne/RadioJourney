package com.myproject.radiojourney.presentation.model

import com.google.android.gms.maps.model.LatLng
import com.myproject.radiojourney.domain.model.Country

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
    countryName = countryName,
    countryLocation = LatLng(latitude, longitude)
)