package com.myproject.radiojourney.presentation.model

import android.os.Parcelable
import com.myproject.radiojourney.domain.model.RadioStation
import kotlinx.parcelize.Parcelize

/**
 * Радиостанция для экрана. Parcelable - передаётся между экранами как аргумент навигации.
 * Поля val: список станций не меняется на месте, при изменении (например, звезды) создаётся копия (copy)
 */
@Parcelize
data class RadioStationPresentation(
    val stationuuid: String,
    val stationName: String,
    val urlResolved: String,
    val clickCount: Int,
    val countryCode: String,
    val country: String,
    val isStationInFavourite: Boolean
) : Parcelable

fun RadioStation.toPresentation() = RadioStationPresentation(
    stationuuid = stationUuid,
    stationName = name,
    urlResolved = urlResolved,
    clickCount = clickCount,
    countryCode = countryCode,
    country = country,
    isStationInFavourite = isFavourite
)

fun RadioStationPresentation.toDomain() = RadioStation(
    stationUuid = stationuuid,
    name = stationName,
    urlResolved = urlResolved,
    clickCount = clickCount,
    country = country,
    countryCode = countryCode,
    isFavourite = isStationInFavourite
)