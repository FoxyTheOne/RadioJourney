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
import com.myproject.radiojourney.other.Constants.POPULAR_STATIONS_FALLBACK_COUNT
import com.myproject.radiojourney.other.Resource
import com.myproject.radiojourney.other.ServerError
import com.myproject.radiojourney.other.Status
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject

/**
 * Data layer, главный репозиторий радио: страны для карты, списки станций, последняя станция.
 *
 * Repository - объект, который решает, откуда взять данные. Здесь это видно лучше всего в getRadioStationList:
 * сначала сервер, при неудаче - список, сохранённый в телефоне, а если и его нет - несколько самых популярных станций.
 * Экран и domain об этом не знают, они получают готовый RadioStationList.
 *
 * Наружу отдаёт только модели domain: преобразование remote / local -> domain происходит здесь (см. data/mapper/DataMappers.kt)
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

    // Список станций страны: с сервера, а если сервер недоступен - сохранённый при прошлом удачном скачивании
    // или (если ответ сервера обрывается) только самые популярные станции.
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

        // Сервер не отдал список. Если этот список уже скачивали раньше - отдаём сохранённый (с датой сохранения):
        // полный, пусть и не самый свежий, лучше, чем короткий
        val savedStations = localRadioDataSource.getSavedStationList(listCountryCode)
        if (savedStations.isNotEmpty()) {
            return Resource.success(
                RadioStationList(
                    savedStations.map { it.toDomain() },
                    savedAt = savedStations.first().savedAt
                )
            )
        }

        // Сохранённого нет, а ответ сервера обрывался на середине - просим только самые популярные станции.
        // Такой ответ маленький (~11 КБ) и проходит там, где большой обрывается (см. POPULAR_STATIONS_FALLBACK_COUNT).
        // Его не сохраняем: он неполный и не должен заменить собой полный список, когда тот удастся скачать
        val failure = ServerError.fromMessage(radioStationRemoteListResource.message)
        if (failure == ServerError.CONNECTION_CUT) {
            val popularStations = networkRadioDataSource.getRadioStationList(
                listCountryCode,
                POPULAR_STATIONS_FALLBACK_COUNT
            ).data
            if (!popularStations.isNullOrEmpty()) {
                return Resource.success(
                    RadioStationList(
                        popularStations.map { it.toDomain() },
                        isOnlyPopular = true
                    )
                )
            }
        }

        // Ничего не вышло. Причину неудачи (ServerError) передаём дальше как есть - по ней экран выберет текст сообщения
        return Resource.error(failure.name, null)
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