package com.myproject.radiojourney.data.dataSource.local.favorite

import com.myproject.radiojourney.data.localDatabaseRoom.IRadioStationDAO
import com.myproject.radiojourney.data.sharedPreference.IAppSharedPreference
import com.myproject.radiojourney.model.local.RadioStationLocal
import javax.inject.Inject

/**
 * LocalFavoriteDataSource Будет доставать данные, либо сохранять их в локальную базу данных (SharedPreference, Room)
 */
class LocalFavoriteDataSource @Inject constructor(
    private val preference: IAppSharedPreference,
    private val radioStationDAO: IRadioStationDAO
) : ILocalFavoriteDataSource {
    // Поменять в Shared Preference setIsRadioStationStored на true. Сохранить в Shared Preference (url)
    override suspend fun saveFavouriteRadioStationUrl(isStored: Boolean, url: String) {
        // Поменять в Shared Preference setIsRadioStationStored на true
        preference.setIsRadioStationStored(isStored)
        // Сохранить в Shared Preference (url)
        preference.saveRadioStationUrl(url)
    }

    override suspend fun getFavoriteRadioStationList(isStationInFavorite: Boolean): List<RadioStationLocal> =
        radioStationDAO.getFavoriteRadioStationList(isStationInFavorite)
}