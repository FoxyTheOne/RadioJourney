package com.myproject.radiojourney.data.repository

import android.content.Context
import android.util.Log
import com.myproject.radiojourney.data.dataSource.local.favorite.ILocalFavoriteDataSource
import com.myproject.radiojourney.data.dataSource.local.radio.ILocalRadioDataSource
import com.myproject.radiojourney.data.dataSource.network.INetworkRadioDataSource
import com.myproject.radiojourney.domain.iRepository.IMainRadioStationRepository
import com.myproject.radiojourney.entities.local.CountryLocal
import com.myproject.radiojourney.entities.local.RadioStationLocal
import com.myproject.radiojourney.utils.exoplayer.FirebaseMusicSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Data layer, Repository. Работает с Local и Remote data source.
 *
 * Repository - объект, предоставляющий доступ к данным с возможностью выбора источника данных в зависимости от условий.
 * Подписка на локальную базу данных Room. Раскладываем данные.
 * При работе с model, здесь происходит запрос в remote, преобразование remote -> local, сохранение результата в базу данных.
 */
class MainRadioStationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkRadioDataSource: INetworkRadioDataSource,
    private val localRadioDataSource: ILocalRadioDataSource,
    private val localFavoriteDataSource: ILocalFavoriteDataSource,
    private val firebaseMusicSource: FirebaseMusicSource
) : IMainRadioStationRepository {
    companion object {
        private const val TAG = "ContentRepository"
    }

    override fun subscribeOnCountryList(): Flow<List<CountryLocal>> =
        localRadioDataSource.subscribeOnCountryList()

    override suspend fun isRadioStationStored(): Boolean =
        localRadioDataSource.isRadioStationStored()

    override suspend fun getRadioStationUrl(): String? = localRadioDataSource.getRadioStationUrl()

    override suspend fun getRadioStationSaved(radioStationUrl: String): RadioStationLocal? =
        localRadioDataSource.getRadioStationSaved(radioStationUrl)

    // Поменять в Shared Preference setIsRadioStationStored на true. Сохранить в Shared Preference (url)
    override suspend fun saveRadioStationUrl(isStored: Boolean, url: String) =
        localFavoriteDataSource.saveFavouriteRadioStationUrl(isStored, url)

    // И сохранить радиостанцию в Room
    override suspend fun saveRadioStationInRoom(radioStationLocal: RadioStationLocal) {
        localRadioDataSource.saveRadioStationInRoom(radioStationLocal)
    }

    override suspend fun getRadioStationList(countryCode: String): List<RadioStationLocal> {
        // Получаем список радиостанций из networkRadioDataSource
        val radioStationRemoteList = networkRadioDataSource.getRadioStationList(countryCode)

        // Преобразуем модельки remote -> local
        val radioStationLocalList = mutableListOf<RadioStationLocal>()

        radioStationRemoteList.forEach { radioStationRemote ->
            val radioStationLocal = RadioStationLocal.fromRemoteToLocal(radioStationRemote)
            radioStationLocalList.add(radioStationLocal)
        }

        Log.d(
            TAG,
            "Успешный запрос; результат запроса радиостанций $radioStationRemoteList, элемент[0]: ${radioStationLocalList[0]}"
        )

        return radioStationLocalList.toList()
    }

    override suspend fun saveLastUsedRadioStationUrlAndCode(url: String, countryCode: String) {
        localRadioDataSource.saveLastUsedRadioStationUrlAndCode(url, countryCode)
    }
}