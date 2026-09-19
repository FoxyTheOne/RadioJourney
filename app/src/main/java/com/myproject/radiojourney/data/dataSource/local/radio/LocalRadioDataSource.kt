package com.myproject.radiojourney.data.dataSource.local.radio

import android.util.Log
import com.myproject.radiojourney.data.localDatabaseRoom.ICountryDAO
import com.myproject.radiojourney.data.localDatabaseRoom.IRadioStationDAO
import com.myproject.radiojourney.data.sharedPreference.IAppSharedPreference
import com.myproject.radiojourney.entities.local.CountryLocal
import com.myproject.radiojourney.entities.local.RadioStationLocal
import kotlinx.coroutines.flow.Flow
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

    override suspend fun setStationFavourite(radioStation: RadioStationLocal, isFavourite: Boolean) =
        radioStationDAO.setStationFavourite(radioStation, isFavourite)

    override suspend fun saveCountryList(countryLocalList: List<CountryLocal>) =
        countryDAO.saveCountryList(*countryLocalList.toTypedArray())

    override suspend fun saveLastUsedRadioStationUrlAndCode(urlResolved: String, countryCode: String) {
        preference.saveLastUsedRadioStationUrl(urlResolved)
        preference.saveLastUsedRadioStationCountryCode(countryCode)
    }

    override suspend fun setIsHideInfoClicked(isHideInfoClicked: Boolean) =
        preference.setIsHideInfoClicked(isHideInfoClicked)

    override suspend fun isHideInfoClicked(): Boolean = preference.isHideInfoClicked()
}