package com.myproject.radiojourney.domain.mainRadioUseCase

import androidx.media3.common.MediaItem
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation

interface IMainRadioUseCase {
    suspend fun mediaItemChildrenToRadioStationPresentation(children: List<MediaItem>): List<RadioStationPresentation>
    suspend fun saveLastUsedRadioStationUrlAndCode(urlResolved: String, countryCode: String)
    suspend fun markRadioStationAsPopularSendGetRequest(stationUuid: String): Boolean
}