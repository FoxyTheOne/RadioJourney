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

    // Станция с таким адресом потока уже добавлена
    suspend fun hasStationWithUrl(urlResolved: String): Boolean

    suspend fun saveMyStation(station: RadioStation)

    suspend fun deleteMyStation(stationUuid: String)
}