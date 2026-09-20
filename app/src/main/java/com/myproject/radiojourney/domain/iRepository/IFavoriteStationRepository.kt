package com.myproject.radiojourney.domain.iRepository

import com.myproject.radiojourney.domain.model.RadioStation

/**
 * Репозиторий избранного: станции со звездой из Room, в моделях domain
 */
interface IFavoriteStationRepository {
    suspend fun getFavoriteRadioStationList(): List<RadioStation>
}