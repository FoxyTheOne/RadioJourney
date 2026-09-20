package com.myproject.radiojourney.data.dataSource.local.radio

import com.myproject.radiojourney.data.localDatabaseRoom.ICountryDAO
import com.myproject.radiojourney.data.localDatabaseRoom.IRadioStationDAO
import com.myproject.radiojourney.data.sharedPreference.IAppSharedPreference
import com.myproject.radiojourney.data.localDatabaseRoom.entity.CountryLocal
import com.myproject.radiojourney.data.localDatabaseRoom.entity.RadioStationLocal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Data layer, Local data source. Работа с Room и Shared Preference
 */
class LocalRadioDataSource @Inject constructor(
    private val countryDAO: ICountryDAO,
    private val radioStationDAO: IRadioStationDAO,
    private val preference: IAppSharedPreference
) : ILocalRadioDataSource {
    override fun subscribeOnCountryList(): Flow<List<CountryLocal>> =
        countryDAO.getCountryList()

    override suspend fun getRadioStationSaved(radioStationUuid: String): RadioStationLocal? =
        radioStationDAO.getRadioStationByUuid(radioStationUuid)

    override suspend fun setStationFavourite(
        radioStation: RadioStationLocal,
        isFavourite: Boolean
    ) =
        radioStationDAO.setStationFavourite(radioStation, isFavourite)

    override suspend fun replaceCountryList(countryLocalList: List<CountryLocal>) =
        countryDAO.replaceCountryList(countryLocalList)

    override suspend fun saveLastUsedRadioStationUrlAndCode(
        urlResolved: String,
        countryCode: String
    ) {
        preference.saveLastUsedRadioStationUrl(urlResolved)
        preference.saveLastUsedRadioStationCountryCode(countryCode)
    }

    override fun getLastUsedRadioStationUrl(): String = preference.getLastUsedRadioStationUrl()

    override fun getLastUsedRadioStationCountryCode(): String =
        preference.getLastUsedRadioStationCountryCode()

    // suspend-функции безопасно вызывать из главного потока: SharedPreferences читает файл с диска в IO
    override suspend fun setIsHideInfoClicked(isHideInfoClicked: Boolean) =
        withContext(Dispatchers.IO) {
            preference.setIsHideInfoClicked(isHideInfoClicked)
        }

    override suspend fun isHideInfoClicked(): Boolean =
        withContext(Dispatchers.IO) { preference.isHideInfoClicked() }
}
