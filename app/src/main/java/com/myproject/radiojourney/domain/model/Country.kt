package com.myproject.radiojourney.domain.model

/**
 * Страна с радиостанциями (маркер на карте) - модель domain слоя.
 * Координаты - обычные числа, а не LatLng: LatLng - класс библиотеки Google Maps, domain от неё не зависит
 */
data class Country(
    val countryCode: String,
    val stationCount: Int,
    val countryName: String,
    val latitude: Double,
    val longitude: Double
)