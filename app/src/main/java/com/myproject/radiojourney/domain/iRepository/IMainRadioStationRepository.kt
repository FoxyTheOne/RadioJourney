package com.myproject.radiojourney.domain.iRepository

import com.myproject.radiojourney.domain.model.Country
import com.myproject.radiojourney.domain.model.RadioStation
import com.myproject.radiojourney.other.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Главный репозиторий радио: страны для карты, станции страны с сервера, избранное и последняя станция.
 *
 * Интерфейс объявлен в domain, реализация - в data (MainRadioStationRepository). Все методы работают
 * с моделями domain: ни экран, ни бизнес-логика не знают, пришли данные из сети, из Room или из настроек
 */
interface IMainRadioStationRepository {
    fun subscribeOnCountryList(): Flow<List<Country>>

    // Станция, сохранённая в Room (например, в избранном), или null
    suspend fun getSavedRadioStation(stationUuid: String): RadioStation?

    // Добавить в избранное или убрать из него: меняется только флаг избранного
    suspend fun setStationFavourite(radioStation: RadioStation, isFavourite: Boolean)

    // Станции страны с сервера. Ошибка сервера - Resource.error
    suspend fun getRadioStationList(countryCode: String): Resource<List<RadioStation>>

    suspend fun saveLastUsedRadioStationUrlAndCode(urlResolved: String, countryCode: String)
    suspend fun getLastUsedRadioStationUrl(): String
    suspend fun getLastUsedRadioStationCountryCode(): String

    suspend fun markRadioStationAsPopularSendGetRequest(stationUuid: String): Boolean

    suspend fun setIsHideInfoClicked(isHideInfoClicked: Boolean)

    // Скрыт ли информационный блок на главном экране (значение меняется - Flow пришлёт новое)
    fun isHideInfoClicked(): Flow<Boolean>
}