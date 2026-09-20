package com.myproject.radiojourney.domain.model

/**
 * Радиостанция - модель domain слоя.
 *
 * Раньше domain работал сразу с моделями других слоёв: RadioStationLocal (таблица Room), RadioStationPresentation (экран)
 * и даже MediaItem из media3. По чистой архитектуре domain - центр приложения и не зависит ни от базы, ни от экрана,
 * ни от библиотек: здесь только Kotlin. Data и presentation слои сами переводят свои модели в эту и обратно
 *
 * @param countryCode код страны. У станций плейлиста избранного - с суффиксом "_FAV" (так плеер отличает плейлист избранного)
 */
data class RadioStation(
    val stationUuid: String,
    val name: String,
    val urlResolved: String,
    val clickCount: Int,
    val country: String,
    val countryCode: String,
    val isFavourite: Boolean
)