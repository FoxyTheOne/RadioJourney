package com.myproject.radiojourney.data.dataSource.network.entity

import com.google.gson.annotations.SerializedName

/**
 * Страна в том виде, в котором её присылает сервер (/json/countries).
 *
 * Раньше приложение брало список стран из /json/countrycodes. С версии API 0.7.23 (апрель 2022) этот запрос
 * помечен как устаревший (DEPRECATED): "используйте countries - там есть и название, и код страны".
 * Он всё ещё работает, но его могут убрать в любой момент - тогда карта осталась бы без стран.
 *
 * Пример ответа: {"name":"Andorra","iso_3166_1":"AD","stationcount":12}
 *
 * Переиспользование: пример модели remote-слоя - такие модели не должны уезжать в экраны, для этого есть domain-модели
 */
data class CountryRemote(
    // Название страны по-английски. На карте показываем название на языке телефона (Locale), а это - запасной вариант
    @SerializedName("name") val name: String?,
    // Код страны ISO 3166-1 (две буквы, например "AD")
    @SerializedName("iso_3166_1") val countryCode: String?,
    @SerializedName("stationcount") val stationCount: Int
)