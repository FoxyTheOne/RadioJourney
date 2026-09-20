package com.myproject.radiojourney.domain.myStationsUseCase

import com.myproject.radiojourney.domain.iRepository.IMyStationRepository
import com.myproject.radiojourney.domain.model.RadioStation
import com.myproject.radiojourney.other.Constants.MY_STATIONS_COUNTRY_CODE
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin. Работает только с Repository
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
    override suspend fun addMyStation(name: String, url: String): IMyStationsUseCase.AddResult {
        val stationName = name.trim()
        // Пробелы по краям часто попадают при вставке ссылки из браузера или мессенджера
        val stationUrl = url.trim()

        return when {
            stationName.isEmpty() -> IMyStationsUseCase.AddResult.EMPTY_NAME
            !isStreamUrlValid(stationUrl) -> IMyStationsUseCase.AddResult.INVALID_URL
            myStationRepository.hasStationWithUrl(stationUrl) -> IMyStationsUseCase.AddResult.DUPLICATE_URL
            else -> {
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
                IMyStationsUseCase.AddResult.ADDED
            }
        }
    }

    override suspend fun deleteMyStation(stationUuid: String) =
        myStationRepository.deleteMyStation(stationUuid)

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