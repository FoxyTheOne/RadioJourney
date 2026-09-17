package com.myproject.radiojourney.domain.mainRadioUseCase

import androidx.media3.common.MediaItem
import com.myproject.radiojourney.domain.iRepository.IMainRadioStationRepository
import com.myproject.radiojourney.entities.local.RadioStationLocal
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import com.myproject.radiojourney.utils.exoplayer.FirebaseMusicSource
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin. Работает только с Repository.
 *
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor).
 * При работе с model, здесь происходит преобразование local -> presentation, т.е.
 * преобразование моделей в модели нижнего уровня перед тем, как нижний уровень сможет их использовать.
 */
class MainRadioUseCase @Inject constructor(
    private val mainRadioStationRepository: IMainRadioStationRepository
) : IMainRadioUseCase {
    companion object {
        private const val TAG = "MainRadioUseCase"
    }

    private suspend fun getRadioStationSaved(radioStationUrlResolved: String): RadioStationLocal? =
        mainRadioStationRepository.getRadioStationSaved(radioStationUrlResolved)

    override suspend fun mediaItemChildrenToRadioStationPresentation(children: List<MediaItem>) =
        children.map {
            // Ищем, может такая радиостанция уже сохранена в Room
            val radioStation = getRadioStationSaved(it.mediaId)
            // Адрес потока, число прослушиваний и страна - в extras (media3 не передаёт адрес потока из сервиса на экран)
            val extras = it.mediaMetadata.extras

            RadioStationPresentation(
                stationuuid = it.mediaId,
                stationName = it.mediaMetadata.title.toString(),
                urlResolved = extras?.getString(FirebaseMusicSource.EXTRA_URL_RESOLVED) ?: "",
                clickCount = extras?.getLong(FirebaseMusicSource.EXTRA_CLICK_COUNT)
                    ?.toInt()
                    ?: 0,
                countryCode = it.mediaMetadata.subtitle.toString(),
                country = extras?.getString(FirebaseMusicSource.EXTRA_COUNTRY)
                    ?: "",
                // Мы получили новые, скачанные из интернета файлы в FirebaseMusicSource.fetchMediaData(), преобразованные в asMediaItems(), переданные из MusicService: result.sendResult() в MainViewModel: musicServiceConnection.subscribe()
                // Т.е. мы не знаем, есть они в Избранном/Рекомендуемом или нет. Нужно проверять и проставлять здесь
                isStationInFavourite = radioStation?.isStationInFavourite
                    ?: false, // Если станция уже сохранена, узнаём её isStationInFavorite, если нет - false
                isStationInRecommended = radioStation?.isStationInRecommended
                    ?: false // Если станция уже сохранена, узнаём её isStationInRecommended, если нет - false
            )
        }

    override suspend fun saveLastUsedRadioStationUrlAndCode(urlResolved: String, countryCode: String) {
        mainRadioStationRepository.saveLastUsedRadioStationUrlAndCode(urlResolved, countryCode)
    }

    override suspend fun markRadioStationAsPopularSendGetRequest(stationUuid: String): Boolean {
        return mainRadioStationRepository.markRadioStationAsPopularSendGetRequest(stationUuid)
    }
}