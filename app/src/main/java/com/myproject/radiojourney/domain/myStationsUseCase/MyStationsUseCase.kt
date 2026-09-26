package com.myproject.radiojourney.domain.myStationsUseCase

import com.myproject.radiojourney.domain.iRepository.IMyStationRepository
import com.myproject.radiojourney.domain.model.RadioStation
import com.myproject.radiojourney.other.Constants.MY_STATIONS_COUNTRY_CODE
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/**
 * Domain layer, UseCase экрана "Мои радиостанции": список своих станций, добавление, правка и удаление.
 *
 * Здесь же проверки того, что ввёл пользователь: название не пустое, ссылка похожа на адрес потока
 * и такой ссылки ещё нет в списке. Это правила приложения, а не оформление экрана, поэтому им место в domain
 */
class MyStationsUseCase @Inject constructor(
    private val myStationRepository: IMyStationRepository
) : IMyStationsUseCase {

    override fun getMyStationList(): Flow<List<RadioStation>> =
        myStationRepository.getMyStationList()

    /**
     * Добавить свою станцию. Название и ссылку пользователь вводит руками, поэтому проверяем их здесь,
     * а не надеемся на плеер: неверную ссылку он покажет только ошибкой воспроизведения, и будет непонятно,
     * станция не работает или адрес набран с опечаткой
     */
    override suspend fun addMyStation(name: String, url: String): IMyStationsUseCase.AddResult =
        checkAndRun(name, url, editedStationUuid = null) { stationName, stationUrl ->
            myStationRepository.saveMyStation(
                RadioStation(
                    // Свою станцию в каталоге radio-browser никто не знает, поэтому uuid выдаём сами.
                    // Он нужен плееру (mediaId) и для удаления станции из списка
                    stationUuid = UUID.randomUUID().toString(),
                    name = stationName,
                    urlResolved = stationUrl,
                    clickCount = 0,
                    country = "",
                    countryCode = MY_STATIONS_COUNTRY_CODE,
                    isFavourite = false
                )
            )
        }

    /**
     * Изменить название или ссылку уже добавленной станции. Проверки те же, что и при добавлении,
     * но станция не считается дублем самой себя, если ссылку не меняли
     */
    override suspend fun editMyStation(
        stationUuid: String,
        name: String,
        url: String
    ): IMyStationsUseCase.AddResult =
        checkAndRun(name, url, editedStationUuid = stationUuid) { stationName, stationUrl ->
            myStationRepository.updateMyStation(stationUuid, stationName, stationUrl)
        }

    override suspend fun deleteMyStation(stationUuid: String) =
        myStationRepository.deleteMyStation(stationUuid)

    /**
     * Общие проверки для добавления и для правки: пустое название, ссылка не похожа на адрес потока,
     * такая ссылка уже есть у другой станции. Если всё хорошо - выполняем [save].
     *
     * [editedStationUuid] - станция, которую редактируем (null, когда добавляем новую):
     * её собственная ссылка дублем не считается
     */
    private suspend fun checkAndRun(
        name: String,
        url: String,
        editedStationUuid: String?,
        save: suspend (name: String, url: String) -> Unit
    ): IMyStationsUseCase.AddResult {
        val stationName = name.trim()
        // Пробелы по краям часто попадают при вставке ссылки из браузера или мессенджера
        val stationUrl = url.trim()

        return when {
            stationName.isEmpty() -> IMyStationsUseCase.AddResult.EMPTY_NAME
            !isStreamUrlValid(stationUrl) -> IMyStationsUseCase.AddResult.INVALID_URL
            isUrlTakenByAnotherStation(
                stationUrl,
                editedStationUuid
            ) -> IMyStationsUseCase.AddResult.DUPLICATE_URL

            else -> {
                save(stationName, stationUrl)
                IMyStationsUseCase.AddResult.ADDED
            }
        }
    }

    private suspend fun isUrlTakenByAnotherStation(
        url: String,
        editedStationUuid: String?
    ): Boolean {
        val ownerUuid = myStationRepository.findStationUuidByUrl(url)
        return ownerUuid != null && ownerUuid != editedStationUuid
    }

    // Минимальная проверка: это http(s)-адрес с именем сервера. Строже проверять смысла нет - работает ссылка или нет,
    // выяснится только при попытке включить станцию
    private fun isStreamUrlValid(url: String): Boolean {
        val isHttp = url.startsWith("http://", ignoreCase = true) || url.startsWith(
            "https://",
            ignoreCase = true
        )
        val hasHost = url.substringAfter("//").substringBefore("/").contains(".")
        return isHttp && hasHost
    }
}