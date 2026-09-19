package com.myproject.radiojourney.data.repository

import android.util.Log
import com.myproject.radiojourney.data.dataSource.local.favorite.ILocalFavoriteDataSource
import com.myproject.radiojourney.data.dataSource.local.radio.ILocalRadioDataSource
import com.myproject.radiojourney.data.dataSource.network.INetworkRadioDataSource
import com.myproject.radiojourney.domain.iRepository.IMainRadioStationRepository
import com.myproject.radiojourney.entities.local.CountryLocal
import com.myproject.radiojourney.entities.local.RadioStationLocal
import com.myproject.radiojourney.other.Constants
import com.myproject.radiojourney.other.Resource
import com.myproject.radiojourney.other.Status
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
    private val networkRadioDataSource: INetworkRadioDataSource,
    private val localRadioDataSource: ILocalRadioDataSource
) : IMainRadioStationRepository {
    override fun subscribeOnCountryList(): Flow<List<CountryLocal>> =
        localRadioDataSource.subscribeOnCountryList()

    override suspend fun getRadioStationSaved(radioStationUuid: String): RadioStationLocal? =
        localRadioDataSource.getRadioStationSaved(radioStationUuid)

    override suspend fun setStationFavourite(radioStationLocal: RadioStationLocal, isFavourite: Boolean) =
        localRadioDataSource.setStationFavourite(radioStationLocal, isFavourite)

    override suspend fun getRadioStationList(countryCode: String): Resource<List<RadioStationLocal>> {
        // Получаем список радиостанций из networkRadioDataSource в формате Resource чтобы знать ответ с сервера
        val radioStationRemoteListResource = networkRadioDataSource.getRadioStationList(countryCode)
        val radioStationRemoteList = radioStationRemoteListResource.data

        // Преобразуем модельки remote -> local
        return if (radioStationRemoteListResource.status == Status.SUCCESS && radioStationRemoteList != null) {
            Resource.success(radioStationRemoteList.map { RadioStationLocal.fromRemoteToLocal(it) })
        } else {
            Resource.error(Constants.SERVER_IS_DOWN, listOf())
        }
    }

    override suspend fun saveLastUsedRadioStationUrlAndCode(urlResolved: String, countryCode: String) =
        localRadioDataSource.saveLastUsedRadioStationUrlAndCode(urlResolved, countryCode)

    // Как указано автором API: send /json/url requests for every click the user makes, this helps to mark stations as popular
    override suspend fun markRadioStationAsPopularSendGetRequest(stationUuid: String): Boolean =
        networkRadioDataSource.sendGetRequestToMarkRadioStationAsPopular(stationUuid)

    override suspend fun setIsHideInfoClicked(isHideInfoClicked: Boolean) =
        localRadioDataSource.setIsHideInfoClicked(isHideInfoClicked)

    override suspend fun isHideInfoClicked(): Boolean = localRadioDataSource.isHideInfoClicked()
}