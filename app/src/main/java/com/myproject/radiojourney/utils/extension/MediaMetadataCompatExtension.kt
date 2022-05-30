package com.myproject.radiojourney.utils.extension

import android.support.v4.media.MediaMetadataCompat
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation

fun MediaMetadataCompat.toRadioStationPresentation(): RadioStationPresentation? {
    return description.let {
        RadioStationPresentation(
            stationName = it.title.toString(),
            url = it.mediaId ?: "",
            urlResolved = it.mediaUri.toString(),
            clickCount = 1, // TODO
            countryCode = it.description.toString(),
            isStationInFavourite = false, // TODO
            isStationInRecommended = false // TODO
        )
    }
}