package com.myproject.radiojourney.data.dataSource.local.radio

import com.myproject.radiojourney.data.localDatabaseRoom.ICountryDAO
import com.myproject.radiojourney.data.localDatabaseRoom.IRadioStationDAO
import com.myproject.radiojourney.data.localDatabaseRoom.ISavedStationDAO
import com.myproject.radiojourney.data.localDatabaseRoom.entity.CountryLocal
import com.myproject.radiojourney.data.localDatabaseRoom.entity.RadioStationLocal
import com.myproject.radiojourney.data.localDatabaseRoom.entity.SavedStationLocal
import com.myproject.radiojourney.data.preference.IAppPreferenceStorage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Data layer, Local data source радио: страны, сохранённые списки станций и избранное - в Room,
 * настройки и последняя станция - в DataStore (раньше SharedPreferences, см. AppPreferenceStorage)
 */
class LocalRadioDataSource @Inject constructor(
    private val countryDAO: ICountryDAO,
    private val radioStationDAO: IRadioStationDAO,
    private val savedStationDAO: ISavedStationDAO,
    private val preferenceStorage: IAppPreferenceStorage
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

    override suspend fun getSavedStationList(countryCode: String): List<SavedStationLocal> =
        savedStationDAO.getStationList(countryCode)

    override suspend fun replaceSavedStationList(
        countryCode: String,
        stations: List<SavedStationLocal>
    ) =
        savedStationDAO.replaceStationList(countryCode, stations)

    override suspend fun saveLastUsedRadioStationUrlAndCode(
        urlResolved: String,
        countryCode: String
    ) {
        preferenceStorage.saveLastUsedRadioStationUrl(urlResolved)
        preferenceStorage.saveLastUsedRadioStationCountryCode(countryCode)
    }

    override suspend fun getLastUsedRadioStationUrl(): String =
        preferenceStorage.getLastUsedRadioStationUrl()

    override suspend fun getLastUsedRadioStationCountryCode(): String =
        preferenceStorage.getLastUsedRadioStationCountryCode()

    override suspend fun setIsHideInfoClicked(isHideInfoClicked: Boolean) =
        preferenceStorage.setIsHideInfoClicked(isHideInfoClicked)

    override fun isHideInfoClicked(): Flow<Boolean> = preferenceStorage.isHideInfoClicked
}