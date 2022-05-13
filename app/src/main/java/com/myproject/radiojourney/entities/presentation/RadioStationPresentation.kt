package com.myproject.radiojourney.entities.presentation

import android.os.Parcelable
import com.myproject.radiojourney.entities.local.RadioStationLocal
import kotlinx.parcelize.Parcelize

@Parcelize
data class RadioStationPresentation(
    val stationName: String,
    val url: String,
    val urlResolved: String,
    val clickCount: Int,
    val countryCode: String,
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
                url = radioStationLocal.url,
                urlResolved = radioStationLocal.urlResolved,
                clickCount = radioStationLocal.clickCount,
                countryCode = radioStationLocal.countryCode,
                isStationInFavourite = isStationInFavourite,
                isStationInRecommended = isStationInRecommended
            )
    }

}