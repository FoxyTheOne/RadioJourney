package com.myproject.radiojourney.domain.mainRadioUseCase

import android.support.v4.media.MediaBrowserCompat
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation

interface IMainRadioUseCase {
    suspend fun mediaItemChildrenToRadioStationPresentation(children: MutableList<MediaBrowserCompat.MediaItem>): List<RadioStationPresentation>
    suspend fun saveLastUsedRadioStationUrlAndCode(urlResolved: String, countryCode: String)
    suspend fun markRadioStationAsPopularSendGetRequest(stationUuid: String)
}