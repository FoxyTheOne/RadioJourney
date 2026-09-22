package com.myproject.radiojourney.data.repository

import com.myproject.radiojourney.data.dataSource.local.country.DeviceCountryDataSource
import com.myproject.radiojourney.data.dataSource.local.radio.ILocalRadioDataSource
import com.myproject.radiojourney.data.dataSource.network.INetworkRadioDataSource
import com.myproject.radiojourney.data.mapper.toDomain
import com.myproject.radiojourney.data.mapper.toLocal
import com.myproject.radiojourney.data.mapper.toSavedStationLocal
import com.myproject.radiojourney.domain.iRepository.IMainRadioStationRepository
import com.myproject.radiojourney.domain.model.Country
import com.myproject.radiojourney.domain.model.RadioStation
import com.myproject.radiojourney.domain.model.RadioStationList
import com.myproject.radiojourney.other.Constants.DEFAULT_COUNTRY_CODE
import com.myproject.radiojourney.other.Resource
import com.myproject.radiojourney.other.ServerError
import com.myproject.radiojourney.other.Status
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale
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
    private val localRadioDataSource: ILocalRadioDataSource,
    private val deviceCountryDataSource: DeviceCountryDataSource
) : IMainRadioStationRepository {

    override fun subscribeOnCountryList(): Flow<List<Country>> =
        localRadioDataSource.subscribeOnCountryList()
            .map { countries -> countries.map { it.toDomain() } }

    override suspend fun getSavedRadioStation(stationUuid: String): RadioStation? =
        localRadioDataSource.getRadioStationSaved(stationUuid)?.toDomain()

    override suspend fun setStationFavourite(radioStation: RadioStation, isFavourite: Boolean) =
        localRadioDataSource.setStationFavourite(radioStation.toLocal(), isFavourite)

    // Список станций страны: с сервера, а если сервер недоступен - сохранённый при прошлом удачном скачивании.
    // Здесь репозиторий и делает то, ради чего существует: решает, откуда взять данные
    override suspend fun getRadioStationList(countryCode: String): Resource<RadioStationList> {
        val listCountryCode = countryCode.uppercase(Locale.ROOT)
        // Получаем список радиостанций из networkRadioDataSource в формате Resource чтобы знать ответ с сервера
        val radioStationRemoteListResource =
            networkRadioDataSource.getRadioStationList(listCountryCode)
        val radioStationRemoteList = radioStationRemoteListResource.data

        if (radioStationRemoteListResource.status == Status.SUCCESS && !radioStationRemoteList.isNullOrEmpty()) {
            // Преобразуем модельки remote -> domain и запоминаем список на случай, если в следующий раз сервер не ответит
            val stations = radioStationRemoteList.map { it.toDomain() }
            val savedAt = System.currentTimeMillis()
            localRadioDataSource.replaceSavedStationList(
                listCountryCode,
                stations.mapIndexed { position, station ->
                    station.toSavedStationLocal(
                        listCountryCode,
                        position,
                        savedAt
                    )
                }
            )
            return Resource.success(RadioStationList(stations))
        }

        // Сервер не отдал список. Если этот список уже скачивали раньше - отдаём сохранённый (с датой сохранения)
        val savedStations = localRadioDataSource.getSavedStationList(listCountryCode)
        if (savedStations.isNotEmpty()) {
            return Resource.success(
                RadioStationList(
                    savedStations.map { it.toDomain() },
                    savedAt = savedStations.first().savedAt
                )
            )
        }

        // Сохранённого нет. Причину неудачи (ServerError) передаём дальше как есть - по ней экран выберет текст сообщения
        return Resource.error(
            radioStationRemoteListResource.message ?: ServerError.SERVER_NOT_RESPONDING.name, null
        )
    }

    // Страна для первого плейлиста: та, что знает телефон (сотовая сеть, SIM, регион - см. DeviceCountryDataSource).
    // Берём только страну, которая есть на карте, - у остальных нет станций. Если список стран ещё не скачан
    // (первый запуск), проверить не по чему - берём код как есть. Ничего не подошло - Антарктида, как раньше
    override suspend fun getHomeCountryCode(): String {
        val knownCountryCodes =
            localRadioDataSource.subscribeOnCountryList().first().map { it.countryCode }.toSet()
        return deviceCountryDataSource.getCountryCodeCandidates()
            .firstOrNull { knownCountryCodes.isEmpty() || it in knownCountryCodes }
            ?: DEFAULT_COUNTRY_CODE
    }

    override suspend fun saveLastUsedRadioStationUrlAndCode(
        urlResolved: String,
        countryCode: String
    ) =
        localRadioDataSource.saveLastUsedRadioStationUrlAndCode(urlResolved, countryCode)

    override suspend fun getLastUsedRadioStationUrl(): String =
        localRadioDataSource.getLastUsedRadioStationUrl()

    override suspend fun getLastUsedRadioStationCountryCode(): String =
        localRadioDataSource.getLastUsedRadioStationCountryCode()

    // Как указано автором API: send /json/url requests for every click the user makes, this helps to mark stations as popular
    override suspend fun markRadioStationAsPopularSendGetRequest(stationUuid: String): Boolean =
        networkRadioDataSource.sendGetRequestToMarkRadioStationAsPopular(stationUuid)

    override suspend fun setIsHideInfoClicked(isHideInfoClicked: Boolean) =
        localRadioDataSource.setIsHideInfoClicked(isHideInfoClicked)

    override fun isHideInfoClicked(): Flow<Boolean> = localRadioDataSource.isHideInfoClicked()
}