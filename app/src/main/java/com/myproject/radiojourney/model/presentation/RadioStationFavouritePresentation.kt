package com.myproject.radiojourney.model.presentation

import android.os.Parcelable
import com.myproject.radiojourney.model.local.RadioStationFavouriteLocal
import kotlinx.parcelize.Parcelize

@Parcelize
data class RadioStationFavouritePresentation(
    val url: String,
    val userCreatorId: Int,
    val stationName: String,
    val clickCount: Int,
    val countryCode: String,
    var isStationInFavourite: Boolean
) :
    Parcelable {

    companion object {
        fun fromLocalToPresentation(
            local: RadioStationFavouriteLocal,
            isStationInFavourite: Boolean = local.isStationInFavourite
        ): RadioStationFavouritePresentation =
            RadioStationFavouritePresentation(
                url = local.url,
                userCreatorId = local.userCreatorId,
                stationName = local.stationName,
                clickCount = local.clickCount,
                countryCode = local.countryCode,
                isStationInFavourite = isStationInFavourite
            )
    }

}