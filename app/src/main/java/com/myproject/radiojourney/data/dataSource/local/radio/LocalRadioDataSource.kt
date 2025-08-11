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
 * LocalRadioDataSource Будет доставать данные, либо сохранять их в локальную базу данных (SharedPreference, Room)
 */
class LocalRadioDataSource @Inject constructor(
    private val countryDAO: ICountryDAO,
    private val radioStationDAO: IRadioStationDAO,
    private val preference: IAppSharedPreference
) : ILocalRadioDataSource {
    companion object {
        private const val TAG = "LocalRadioDataSource"
    }

    override fun subscribeOnCountryList(): Flow<List<CountryLocal>> =
        countryDAO.getCountryList()

    override suspend fun isRadioStationStored(): Boolean = preference.isRadioStationStored()

    override suspend fun getRadioStationUrl(): String? = preference.getRadioStationUrl()

    override suspend fun getRadioStationSaved(radioStationUuid: String): RadioStationLocal? =
        radioStationDAO.getRadioStationByUuid(radioStationUuid)

    override suspend fun saveRadioStationInRoom(radioStation: RadioStationLocal) {
        // Сохранить радиостанцию в Room
        radioStationDAO.saveRadioStationList(radioStation)
    }

    override suspend fun saveCountryList(countryLocalList: MutableList<CountryLocal>) {
        countryDAO.saveCountryList(*countryLocalList.toTypedArray())
        Log.d(TAG, "результат Метод для сохранения стран в Room завершён")
    }

    override suspend fun saveLastUsedRadioStationUrlAndCode(
        urlResolved: String,
        countryCode: String
    ) {
        preference.saveLastUsedRadioStationUrl(urlResolved)
        preference.saveLastUsedRadioStationCountryCode(countryCode)
    }

    override suspend fun setIsHideInfoClicked(isHideInfoClicked: Boolean) =
        preference.setIsHideInfoClicked(isHideInfoClicked)

    override suspend fun isHideInfoClicked(): Boolean = preference.isHideInfoClicked()
}