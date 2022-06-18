package com.myproject.radiojourney.domain.mainRadioUseCase

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation

interface IMainRadioUseCase {
    suspend fun mediaItemChildrenToRadioStationPresentation(children: MutableList<MediaBrowserCompat.MediaItem>): List<RadioStationPresentation>
    suspend fun saveLastUsedRadioStationUrlAndCode(url: String, countryCode: String)
}