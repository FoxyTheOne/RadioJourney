package com.myproject.radiojourney.data.dataSource.local.radio

import android.util.Log
import com.myproject.radiojourney.data.localDatabaseRoom.ICountryDAO
import com.myproject.radiojourney.model.local.CountryLocal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * LocalRadioDataSource Будет доставать данные, либо сохранять их в локальную базу данных (SharedPreference, Room)
 */
class LocalRadioDataSource @Inject constructor(
    private val countryDAO: ICountryDAO
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
}