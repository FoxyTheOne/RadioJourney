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
//    @ApplicationContext private val context: Context,
    private val networkRadioDataSource: INetworkRadioDataSource,
    private val localRadioDataSource: ILocalRadioDataSource,
    private val localFavoriteDataSource: ILocalFavoriteDataSource,
//    private val firebaseMusicSource: FirebaseMusicSource
) : IMainRadioStationRepository {
    companion object {
        private const val TAG = "ContentRepository"
    }

    override fun subscribeOnCountryList(): Flow<List<CountryLocal>> =
        localRadioDataSource.subscribeOnCountryList()

    override suspend fun isRadioStationStored(): Boolean =
        localRadioDataSource.isRadioStationStored()

    override suspend fun getRadioStationUrl(): String? = localRadioDataSource.getRadioStationUrl()

    override suspend fun getRadioStationSaved(radioStationUuidResolved: String): RadioStationLocal? =
        localRadioDataSource.getRadioStationSaved(radioStationUuidResolved)

    // Поменять в Shared Preference setIsRadioStationStored на true. Сохранить в Shared Preference (url)
    override suspend fun saveRadioStationUrl(isStored: Boolean, urlResolved: String) =
        localFavoriteDataSource.saveFavouriteRadioStationUrl(isStored, urlResolved)

    // И сохранить радиостанцию в Room
    override suspend fun saveRadioStationInRoom(radioStationLocal: RadioStationLocal) {
        localRadioDataSource.saveRadioStationInRoom(radioStationLocal)
    }

    override suspend fun getRadioStationList(countryCode: String): Resource<List<RadioStationLocal>> {
        // Получаем список радиостанций из networkRadioDataSource в формате Resource чтобы знать ответ с сервера
        val radioStationRemoteListResource = networkRadioDataSource.getRadioStationList(countryCode)

        // Если была ошибка HttpException, обозначаем по умолчанию
        var radioStationLocalListResource: Resource<List<RadioStationLocal>> =
            Resource.error(Constants.SERVER_IS_DOWN, listOf())

        // Далее проверяем и если ошибки HttpException не было - меняем значение
        radioStationRemoteListResource.let { result ->
            when (result.status) {
                Status.SUCCESS -> {
                    result.data?.let { radioStationRemoteList ->

                        // Преобразуем модельки remote -> local
                        val radioStationLocalList = mutableListOf<RadioStationLocal>()

                        radioStationRemoteList.forEach { radioStationRemote ->
                            val radioStationLocal =
                                RadioStationLocal.fromRemoteToLocal(radioStationRemote)
                            radioStationLocalList.add(radioStationLocal)
                        }

                        Log.d(
                            TAG,
                            "Успешный запрос; результат запроса радиостанций $radioStationRemoteList"
                        )

                        radioStationLocalListResource =
                            Resource.success(radioStationLocalList.toList())
                    }
                }

                Status.ERROR -> Unit // we don't need this
                Status.LOADING -> Unit // we don't need this
            }
        }

//        val radioStationRemoteList = networkRadioDataSource.getRadioStationList(countryCode)
//
//        // Преобразуем модельки remote -> local
//        val radioStationLocalList = mutableListOf<RadioStationLocal>()
//
//        radioStationRemoteList.forEach { radioStationRemote ->
//            val radioStationLocal = RadioStationLocal.fromRemoteToLocal(radioStationRemote)
//            radioStationLocalList.add(radioStationLocal)
//        }
//
//        Log.d(
//            TAG,
//            "Успешный запрос; результат запроса радиостанций $radioStationRemoteList"
//        )

        return radioStationLocalListResource
    }

    override suspend fun saveLastUsedRadioStationUrlAndCode(
        urlResolved: String,
        countryCode: String
    ) {
        localRadioDataSource.saveLastUsedRadioStationUrlAndCode(urlResolved, countryCode)
    }

    override suspend fun markRadioStationAsPopularSendGetRequest(stationUuid: String): Boolean {
        Log.d(
            TAG,
            "Делаем запрос, как указано автором API (Send /json/url requests for every click the user makes, this helps to mark stations as popular and makes the database more usefull to other people)"
        )
        return networkRadioDataSource.sendGetRequestToMarkRadioStationAsPopular(stationUuid)
    }


    override suspend fun setIsHideInfoClicked(isHideInfoClicked: Boolean) =
        localRadioDataSource.setIsHideInfoClicked(isHideInfoClicked)

    override suspend fun isHideInfoClicked(): Boolean = localRadioDataSource.isHideInfoClicked()
}