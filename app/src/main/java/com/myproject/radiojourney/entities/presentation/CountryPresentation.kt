package com.myproject.radiojourney.entities.presentation

import com.google.android.gms.maps.model.LatLng
import com.myproject.radiojourney.entities.local.CountryLocal

data class CountryPresentation(
    val countryCode: String,
    val stationCount: Int,
    val countryName: String,
    val countryLocation: LatLng
) {

    companion object {
        fun fromLocalToPresentation(countryLocal: CountryLocal): CountryPresentation =
            CountryPresentation(
                countryCode = countryLocal.countryCode,
                stationCount = countryLocal.stationcount,
                countryName = countryLocal.countryName,
                countryLocation = countryLocal.countryLocation
            )
    }

}