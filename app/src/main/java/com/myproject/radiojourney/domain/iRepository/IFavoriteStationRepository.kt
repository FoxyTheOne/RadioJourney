package com.myproject.radiojourney.domain.iRepository

import com.myproject.radiojourney.entities.local.RadioStationLocal

interface IFavoriteStationRepository {
    suspend fun getFavoriteRadioStationList(isStationInFavorite: Boolean): List<RadioStationLocal>
}