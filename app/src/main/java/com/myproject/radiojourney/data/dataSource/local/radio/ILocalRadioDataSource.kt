package com.myproject.radiojourney.data.dataSource.local.radio

import com.myproject.radiojourney.data.localDatabaseRoom.entity.CountryLocal
import com.myproject.radiojourney.data.localDatabaseRoom.entity.RadioStationLocal
import kotlinx.coroutines.flow.Flow

/**
 * Local data source радио: всё, что приложение хранит на телефоне - страны и станции в Room,
 * настройки и последняя станция в DataStore.
 *
 * Data source отвечает за один источник данных, а репозиторий решает, откуда брать данные (сеть или телефон)
 */
interface ILocalRadioDataSource {
    fun subscribeOnCountryList(): Flow<List<CountryLocal>>

    suspend fun getRadioStationSaved(radioStationUuid: String): RadioStationLocal?

    suspend fun setStationFavourite(radioStation: RadioStationLocal, isFavourite: Boolean)

    // Заменить весь список стран: страны, которых больше нет (или у которых нет координат), удаляются из базы
    suspend fun replaceCountryList(countryLocalList: List<CountryLocal>)

    suspend fun saveLastUsedRadioStationUrlAndCode(urlResolved: String, countryCode: String)
    suspend fun getLastUsedRadioStationUrl(): String
    suspend fun getLastUsedRadioStationCountryCode(): String

    suspend fun setIsHideInfoClicked(isHideInfoClicked: Boolean)

    fun isHideInfoClicked(): Flow<Boolean>
}