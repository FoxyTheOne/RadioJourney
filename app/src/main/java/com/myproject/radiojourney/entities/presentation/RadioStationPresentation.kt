package com.myproject.radiojourney.entities.presentation

import android.os.Parcelable
import com.myproject.radiojourney.entities.local.RadioStationLocal
import kotlinx.parcelize.Parcelize

@Parcelize
data class RadioStationPresentation(
    val stationName: String,
    val urlResolved: String,
    val clickCount: Int,
    var countryCode: String,
    val country: String,
    var isStationInFavourite: Boolean,
    var isStationInRecommended: Boolean
) :
    Parcelable {

    companion object {
        fun fromLocalToPresentation(
            radioStationLocal: RadioStationLocal,
            isStationInFavourite: Boolean = radioStationLocal.isStationInFavourite,
            isStationInRecommended: Boolean = radioStationLocal.isStationInRecommended
        ): RadioStationPresentation =
            RadioStationPresentation(
                stationName = radioStationLocal.stationName,
                urlResolved = radioStationLocal.urlResolved,
                clickCount = radioStationLocal.clickCount,
                country = radioStationLocal.country,
                countryCode = radioStationLocal.countryCode,
                isStationInFavourite = isStationInFavourite,
                isStationInRecommended = isStationInRecommended
            )
    }

}