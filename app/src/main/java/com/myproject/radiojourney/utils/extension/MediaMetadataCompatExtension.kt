package com.myproject.radiojourney.utils.extension

//import android.os.Bundle
//import android.support.v4.media.MediaDescriptionCompat
//import android.support.v4.media.MediaDescriptionCompat.STATUS_DOWNLOADED
//import android.support.v4.media.MediaMetadataCompat
//import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DOWNLOAD_STATUS
//import androidx.core.net.toUri
//import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
//
//fun MediaMetadataCompat.toRadioStationPresentation(): RadioStationPresentation? {
//    return description.let {
//        RadioStationPresentation(
//            stationName = it.title.toString(),
//            url = it.mediaId ?: "",
//            urlResolved = it.mediaUri.toString(),
//            clickCount = it.extras?.getLong("ClickCount")?.toInt() ?: 0, // не работает
//            countryCode = it.extras?.getString("CountryCode") ?: "", // не работает
//            isStationInFavourite = false, // TODO
//            isStationInRecommended = false // TODO
//        )
//    }
//}



