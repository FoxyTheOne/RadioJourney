package com.myproject.radiojourney.domain.iRepository

import com.myproject.radiojourney.domain.model.RadioStation
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий своих станций ("Мои радиостанции"): станции, добавленные пользователем по ссылке.
 *
 * Наружу отдаёт те же domain-модели RadioStation, что и станции из каталога, - поэтому их умеет играть
 * тот же плеер и показывать те же списки. Отличает их код страны MY_STATIONS_COUNTRY_CODE
 */
interface IMyStationRepository {
    // Подписка: после добавления или удаления станции список приходит заново
    fun getMyStationList(): Flow<List<RadioStation>>

    // Список разово - для плеера
    suspend fun getMyStationListOnce(): List<RadioStation>

    // uuid станции с таким адресом потока, если она уже добавлена
    suspend fun findStationUuidByUrl(urlResolved: String): String?

    suspend fun saveMyStation(station: RadioStation)

    // Изменить название и ссылку у уже добавленной станции
    suspend fun updateMyStation(stationUuid: String, name: String, urlResolved: String)

    suspend fun deleteMyStation(stationUuid: String)
}