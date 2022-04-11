package com.myproject.radiojourney.model.presentation

import android.os.Parcelable
import com.myproject.radiojourney.model.local.RadioStationLocal
import kotlinx.parcelize.Parcelize

@Parcelize
data class RadioStationPresentation(
    val stationName: String,
    val url: String,
    val clickCount: Int,
    val countryCode: String,
    var isStationInFavourite: Boolean
) :
    Parcelable {

    companion object {
        fun fromLocalToPresentation(
            radioStationLocal: RadioStationLocal,
            isStationInFavourite: Boolean = radioStationLocal.isStationInFavourite
        ): RadioStationPresentation =
            RadioStationPresentation(
                stationName = radioStationLocal.stationName,
                url = radioStationLocal.url,
                clickCount = radioStationLocal.clickCount,
                countryCode = radioStationLocal.countryCode,
                isStationInFavourite = isStationInFavourite
            )

        fun fromFavouritePresentationToPresentation(
            radioFavouritePresentation: RadioStationFavouritePresentation,
            isStationInFavourite: Boolean = radioFavouritePresentation.isStationInFavourite
        ): RadioStationPresentation =
            RadioStationPresentation(
                stationName = radioFavouritePresentation.stationName,
                url = radioFavouritePresentation.url,
                clickCount = radioFavouritePresentation.clickCount,
                countryCode = radioFavouritePresentation.countryCode,
                isStationInFavourite = isStationInFavourite
            )
    }

}