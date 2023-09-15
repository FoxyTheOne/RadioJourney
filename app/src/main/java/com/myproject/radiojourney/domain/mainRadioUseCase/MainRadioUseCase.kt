package com.myproject.radiojourney.domain.mainRadioUseCase

import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import com.myproject.radiojourney.domain.iRepository.IMainRadioStationRepository
import com.myproject.radiojourney.entities.local.RadioStationLocal
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
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

    override suspend fun mediaItemChildrenToRadioStationPresentation(children: MutableList<MediaBrowserCompat.MediaItem>) =
        children.map {
            // Ищем, может такая радиостанция уже сохранена в Room
            val radioStation = getRadioStationSaved(it.description.mediaId.toString())

            RadioStationPresentation(
                stationuuid = it.mediaId ?: "",
                stationName = it.description.title.toString(),
//                urlResolved = it.mediaId ?: "",
                urlResolved = it.description.mediaUri.toString(),
                clickCount = it.description.extras?.getLong("ClickCount")
                    ?.toInt()
                    ?: 0,
                countryCode = it.description.subtitle.toString(),
                country = it.description.extras?.getString("Country")
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
}