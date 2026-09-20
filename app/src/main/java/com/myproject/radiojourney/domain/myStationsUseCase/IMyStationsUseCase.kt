package com.myproject.radiojourney.domain.myStationsUseCase

import com.myproject.radiojourney.domain.model.RadioStation
import kotlinx.coroutines.flow.Flow

/**
 * UseCase экрана "Мои радиостанции": список своих станций, добавление по ссылке и удаление.
 *
 * Проверка того, что ввёл пользователь, - тоже здесь: это правила приложения, а не оформление экрана,
 * поэтому они лежат в domain и не зависят от Android (их можно проверить обычным unit-тестом)
 */
interface IMyStationsUseCase {

    // Чем закончилась попытка добавить станцию. Экран по этому значению решает, какую подсказку показать
    enum class AddResult {
        ADDED,
        EMPTY_NAME, // не заполнено название
        INVALID_URL, // ссылка не похожа на адрес потока
        DUPLICATE_URL // станция с такой ссылкой уже есть в списке
    }

    fun getMyStationList(): Flow<List<RadioStation>>

    suspend fun addMyStation(name: String, url: String): AddResult

    // Изменить название или ссылку уже добавленной станции
    suspend fun editMyStation(stationUuid: String, name: String, url: String): AddResult

    suspend fun deleteMyStation(stationUuid: String)
}