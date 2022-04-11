package com.myproject.radiojourney.data.dataSource.local.radio

import android.util.Log
import com.myproject.radiojourney.data.localDatabaseRoom.ICountryDAO
import com.myproject.radiojourney.data.localDatabaseRoom.IRadioStationDAO
import com.myproject.radiojourney.data.localDatabaseRoom.IRadioStationFavouriteDAO
import com.myproject.radiojourney.data.sharedPreference.IAppSharedPreference
import com.myproject.radiojourney.model.local.CountryLocal
import com.myproject.radiojourney.model.local.RadioStationFavouriteLocal
import com.myproject.radiojourney.model.local.RadioStationLocal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * LocalRadioDataSource Будет доставать данные, либо сохранять их в локальную базу данных (SharedPreference, Room)
 */
class LocalRadioDataSource @Inject constructor(
    private val countryDAO: ICountryDAO,
    private val radioStationDAO: IRadioStationDAO,
    private val preference: IAppSharedPreference,
    private val radioStationFavouriteDAO: IRadioStationFavouriteDAO
) : ILocalRadioDataSource {
    companion object {
        private const val TAG = "LocalRadioDataSource"
    }

    override suspend fun saveCountryList(countryLocalList: MutableList<CountryLocal>) {
        countryDAO.saveCountryList(*countryLocalList.toTypedArray())
        Log.d(TAG, "результат Метод для сохранения стран в Room завершён")
    }

    override fun subscribeOnCountryList(): Flow<List<CountryLocal>> =
        countryDAO.getCountryList()

    override suspend fun isRadioStationStored(): Boolean = preference.isRadioStationStored()

    override suspend fun getRadioStationUrl(): String? = preference.getRadioStationUrl()

    override suspend fun getRadioStationSaved(radioStationUrl: String): RadioStationLocal? =
        radioStationDAO.getRadioStation(radioStationUrl)

    override suspend fun saveRadioStationUrl(isStored: Boolean, radioStation: RadioStationLocal) {
        // Поменять в Shared Preference setIsRadioStationStored на true
        preference.setIsRadioStationStored(isStored)
        // Сохранить в Shared Preference (url) и Room (станцию)
        preference.saveRadioStationUrl(radioStation.url)
        radioStationDAO.saveRadioStationList(radioStation)
    }

    override suspend fun saveFavouriteRadioStationUrl(isStored: Boolean, url: String) {
        // Поменять в Shared Preference setIsRadioStationStored на true
        preference.setIsRadioStationStored(isStored)
        // Сохранить в Shared Preference (url)
        preference.saveRadioStationUrl(url)
    }

    override suspend fun getToken() = preference.getToken()

    override suspend fun addStationToFavourites(currentRadioStationFavourite: RadioStationFavouriteLocal) {
        radioStationFavouriteDAO.saveRadioStationFavourite(currentRadioStationFavourite)
    }

    override suspend fun isStationInFavourites(url: String): Boolean {
        val favouriteRadioStation = radioStationFavouriteDAO.getRadioStationFavourite(url)
        return favouriteRadioStation != null
    }

    override suspend fun deleteRadioStationFromFavourite(radioStationFavourite: RadioStationFavouriteLocal) =
        radioStationFavouriteDAO.deleteRadioStationFromFavourite(radioStationFavourite)
}