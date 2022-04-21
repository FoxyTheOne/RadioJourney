package com.myproject.radiojourney.data.repository

import android.content.Context
import android.util.Log
import com.myproject.radiojourney.data.dataSource.local.auth.ILocalUserDataSource
import com.myproject.radiojourney.data.dataSource.local.radio.ILocalRadioDataSource
import com.myproject.radiojourney.data.dataSource.network.INetworkRadioDataSource
import com.myproject.radiojourney.domain.iRepository.IContentRepository
import com.myproject.radiojourney.model.local.CountryLocal
import com.myproject.radiojourney.model.local.RadioStationFavouriteLocal
import com.myproject.radiojourney.model.local.RadioStationLocal
import com.myproject.radiojourney.model.local.UserWithStations
import com.myproject.radiojourney.model.presentation.RadioStationPresentation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Repository. Data layer. Работает с Local и Remote data source.
 * Подписка на локальную базу данных Room.
 * При работе с model, здесь происходит запрос в remote, преобразование remote -> local, сохранение результата в базу данных.
 */
class ContentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkRadioDataSource: INetworkRadioDataSource,
    private val localRadioDataSource: ILocalRadioDataSource,
    private val localUserDataSource: ILocalUserDataSource
) : IContentRepository {
    companion object {
        private const val TAG = "ContentRepository"
    }

    override fun subscribeOnCountryList(): Flow<List<CountryLocal>> =
        localRadioDataSource.subscribeOnCountryList()

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
            "Успешный запрос; результат запроса радиостанций[0]: ${radioStationLocalList[0]}"
        )

        return radioStationLocalList.toList()
    }

    override suspend fun isRadioStationStored(): Boolean =
        localRadioDataSource.isRadioStationStored()

    override suspend fun getRadioStationUrl(): String? = localRadioDataSource.getRadioStationUrl()
    override suspend fun getRadioStationSaved(radioStationUrl: String): RadioStationLocal? =
        localRadioDataSource.getRadioStationSaved(radioStationUrl)

    override suspend fun saveRadioStationUrl(
        isStored: Boolean,
        radioStation: RadioStationPresentation
    ) {
        val radioStationLocal = RadioStationLocal.fromPresentationToLocal(radioStation)
        localRadioDataSource.saveRadioStationUrl(isStored, radioStationLocal)
    }

    override suspend fun saveFavouriteRadioStationUrl(isStored: Boolean, url: String) =
        localRadioDataSource.saveFavouriteRadioStationUrl(isStored, url)

    override suspend fun getToken(): Int? {
        // Узнаём userCreatorId
        // В нашем случае userCreatorId = token
        val userCreatorId = localRadioDataSource.getToken()
        val userCreatorIdInt = userCreatorId.toIntOrNull()
        Log.d(TAG, "Результат преобразования $userCreatorId String в Int = $userCreatorIdInt")

        return userCreatorIdInt
    }

    override suspend fun addStationToFavourites(currentRadioStationFavouriteLocal: RadioStationFavouriteLocal) {
        localRadioDataSource.addStationToFavourites(currentRadioStationFavouriteLocal)
    }

    override suspend fun isStationInFavourites(url: String): Boolean =
        localRadioDataSource.isStationInFavourites(url)

    override suspend fun deleteRadioStationFromFavourite(currentRadioStationFavouriteLocal: RadioStationFavouriteLocal) =
        localRadioDataSource.deleteRadioStationFromFavourite(currentRadioStationFavouriteLocal)

    override suspend fun getUsersWithStations(): List<UserWithStations> =
        localUserDataSource.getUsersWithStations()

    override suspend fun getRecommendedRadioStationList(): List<RadioStationLocal> =
        localRadioDataSource.getRecommendedRadioStationList()

    override suspend fun setRecommendedRadioStations() {
        val antyradioURL = "https://n-4-2.dcs.redcdn.pl/sc/o2/Eurozet/live/antyradio.livx?audio=5"
        val antyradio = localRadioDataSource.getRadioStationSaved(antyradioURL)
        if (antyradio != null) {
            antyradio.isStationInRecommended = true
            localRadioDataSource.saveRadioStationList(antyradio)
        } else {
            val radioStationsPL = networkRadioDataSource.getRadioStationList("PL")
            radioStationsPL.forEach { radioStationRemote ->
                if (radioStationRemote.url == antyradioURL) {
                    localRadioDataSource.saveRadioStationList(
                        RadioStationLocal.fromRemoteToLocal(
                            radioStationRemote,
                            isStationInRecommended = true
                        )
                    )
                }
            }
        }

        val easyFMURL = "https://netradio.ziniur.lt/easyfm.mp3"
        val easyFM = localRadioDataSource.getRadioStationSaved(easyFMURL)
        if (easyFM != null) {
            easyFM.isStationInRecommended = true
            localRadioDataSource.saveRadioStationList(easyFM)
        } else {
            val radioStationsLT = networkRadioDataSource.getRadioStationList("LT")
            radioStationsLT.forEach { radioStationRemote ->
                if (radioStationRemote.url == easyFMURL) {
                    localRadioDataSource.saveRadioStationList(
                        RadioStationLocal.fromRemoteToLocal(
                            radioStationRemote,
                            isStationInRecommended = true
                        )
                    )
                }
            }
        }

        val ro90s3NeRgYURL = "https://s11.ssl-stream.com/ssl/90s_energy?mp=/stream"
        val ro90s3NeRgY = localRadioDataSource.getRadioStationSaved(ro90s3NeRgYURL)
        if (ro90s3NeRgY != null) {
            ro90s3NeRgY.isStationInRecommended = true
            localRadioDataSource.saveRadioStationList(ro90s3NeRgY)
        } else {
            val radioStationsRO = networkRadioDataSource.getRadioStationList("RO")
            radioStationsRO.forEach { radioStationRemote ->
                if (radioStationRemote.url == ro90s3NeRgYURL) {
                    localRadioDataSource.saveRadioStationList(
                        RadioStationLocal.fromRemoteToLocal(
                            radioStationRemote,
                            isStationInRecommended = true
                        )
                    )
                }
            }
        }
    }
}