package com.myproject.radiojourney.data.repository

import com.myproject.radiojourney.data.dataSource.local.radio.ILocalRadioDataSource
import com.myproject.radiojourney.data.dataSource.network.INetworkRadioDataSource
import com.myproject.radiojourney.data.mapper.toDomain
import com.myproject.radiojourney.data.mapper.toLocal
import com.myproject.radiojourney.domain.iRepository.IMainRadioStationRepository
import com.myproject.radiojourney.domain.model.Country
import com.myproject.radiojourney.domain.model.RadioStation
import com.myproject.radiojourney.other.Constants
import com.myproject.radiojourney.other.Resource
import com.myproject.radiojourney.other.Status
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Data layer, Repository. Работает с Local и Remote data source.
 *
 * Repository - объект, предоставляющий доступ к данным с возможностью выбора источника данных в зависимости от условий.
 * Подписка на локальную базу данных Room. Раскладываем данные.
 * Отдаёт наружу модели domain: преобразование remote / local -> domain происходит здесь
 */
class MainRadioStationRepository @Inject constructor(
    private val networkRadioDataSource: INetworkRadioDataSource,
    private val localRadioDataSource: ILocalRadioDataSource
) : IMainRadioStationRepository {

    override fun subscribeOnCountryList(): Flow<List<Country>> =
        localRadioDataSource.subscribeOnCountryList().map { countries -> countries.map { it.toDomain() } }

    override suspend fun getSavedRadioStation(stationUuid: String): RadioStation? =
        localRadioDataSource.getRadioStationSaved(stationUuid)?.toDomain()

    override suspend fun setStationFavourite(radioStation: RadioStation, isFavourite: Boolean) =
        localRadioDataSource.setStationFavourite(radioStation.toLocal(), isFavourite)

    override suspend fun getRadioStationList(countryCode: String): Resource<List<RadioStation>> {
        // Получаем список радиостанций из networkRadioDataSource в формате Resource чтобы знать ответ с сервера
        val radioStationRemoteListResource = networkRadioDataSource.getRadioStationList(countryCode)
        val radioStationRemoteList = radioStationRemoteListResource.data

        // Преобразуем модельки remote -> domain
        return if (radioStationRemoteListResource.status == Status.SUCCESS && radioStationRemoteList != null) {
            Resource.success(radioStationRemoteList.map { it.toDomain() })
        } else {
            Resource.error(Constants.SERVER_IS_DOWN, listOf())
        }
    }

    override suspend fun saveLastUsedRadioStationUrlAndCode(urlResolved: String, countryCode: String) =
        localRadioDataSource.saveLastUsedRadioStationUrlAndCode(urlResolved, countryCode)

    override fun getLastUsedRadioStationUrl(): String = localRadioDataSource.getLastUsedRadioStationUrl()

    override fun getLastUsedRadioStationCountryCode(): String = localRadioDataSource.getLastUsedRadioStationCountryCode()

    // Как указано автором API: send /json/url requests for every click the user makes, this helps to mark stations as popular
    override suspend fun markRadioStationAsPopularSendGetRequest(stationUuid: String): Boolean =
        networkRadioDataSource.sendGetRequestToMarkRadioStationAsPopular(stationUuid)

    override suspend fun setIsHideInfoClicked(isHideInfoClicked: Boolean) =
        localRadioDataSource.setIsHideInfoClicked(isHideInfoClicked)

    override suspend fun isHideInfoClicked(): Boolean = localRadioDataSource.isHideInfoClicked()
}