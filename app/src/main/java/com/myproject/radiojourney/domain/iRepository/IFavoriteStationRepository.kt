package com.myproject.radiojourney.domain.iRepository

import com.myproject.radiojourney.domain.model.RadioStation

interface IFavoriteStationRepository {
    suspend fun getFavoriteRadioStationList(): List<RadioStation>
}