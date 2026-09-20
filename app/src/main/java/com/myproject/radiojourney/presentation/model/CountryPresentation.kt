package com.myproject.radiojourney.presentation.model

import com.google.android.gms.maps.model.LatLng
import com.myproject.radiojourney.domain.model.Country

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