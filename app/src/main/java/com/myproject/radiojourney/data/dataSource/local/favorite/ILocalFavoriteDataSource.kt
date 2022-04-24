package com.myproject.radiojourney.data.dataSource.local.favorite

import com.myproject.radiojourney.model.local.RadioStationLocal

interface ILocalFavoriteDataSource {
    // Поменять в Shared Preference setIsRadioStationStored на true. Сохранить в Shared Preference (url)
    suspend fun saveFavouriteRadioStationUrl(isStored: Boolean, url: String)

    suspend fun getFavoriteRadioStationList(isStationInFavorite: Boolean): List<RadioStationLocal>
}