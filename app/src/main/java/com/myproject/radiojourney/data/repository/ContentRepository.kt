package com.myproject.radiojourney.data.repository

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.myproject.radiojourney.data.dataSource.local.radio.ILocalRadioDataSource
import com.myproject.radiojourney.data.dataSource.network.INetworkRadioDataSource
import com.myproject.radiojourney.domain.iRepository.IContentRepository
import com.myproject.radiojourney.model.local.CountryLocal
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import java.util.*
import javax.inject.Inject

/**
 * Repository. Data layer. Работает с Local и Remote data source.
 * Подписка на локальную базу данных Room.
 * При работе с model, здесь происходит запрос в remote, преобразование remote -> local, сохранение результата в базу данных.
 */
class ContentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkRadioDataSource: INetworkRadioDataSource,
    private val localRadioDataSource: ILocalRadioDataSource
) : IContentRepository {
    companion object {
        private const val TAG = "ContentRepository"
    }

    override suspend fun getCountryListAndSaveToRoom() {
        // Получаем список кодов стран из networkRadioDataSource
        val countryCodeRemoteList = networkRadioDataSource.getCountryCodeList()

        // Преобразуем коды (remote) в читабельные страны (local) с локацией
        val countryLocalList = mutableListOf<CountryLocal>()

        val geocoder = Geocoder(context)
        var addresses = mutableListOf<Address>()

        countryCodeRemoteList.forEach { countryCodeRemote ->
            // Узнаем название страны
            val loc: Locale = Locale("", countryCodeRemote.name)
            val countryName = loc.displayName
            Log.d(TAG, "результат countryName: $countryName")

            // Узнаем местоположение
            // В этом месте вылетает, если проблема с интернетом
            addresses = geocoder.getFromLocationName(countryName, 1)
            var latitude: Double = 0.0
            var longitude: Double = 0.0
            if (addresses.size > 0) {
                latitude = addresses[0].latitude;
                longitude = addresses[0].longitude;
            }
            Log.d(TAG, "результат addresses: $latitude, $longitude")

            // remote -> local
            val countryLocal = CountryLocal.fromRemoteToLocal(
                countryCodeRemote,
                countryName = countryName,
                countryLocation = LatLng(latitude, longitude)
            )
            countryLocalList.add(countryLocal)
        }

        countryLocalList.forEach { countryLocal ->
            Log.d(TAG, "результат преобразования названия страны: ${countryLocal.countryName}")
        }

        // Теперь сохраним наши страны в Room
        localRadioDataSource.saveCountryList(countryLocalList)
    }

    override fun subscribeOnCountryList(): Flow<List<CountryLocal>> =
        localRadioDataSource.subscribeOnCountryList()
}