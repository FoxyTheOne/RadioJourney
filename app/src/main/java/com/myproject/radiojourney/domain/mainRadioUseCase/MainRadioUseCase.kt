package com.myproject.radiojourney.domain.mainRadioUseCase

import android.support.v4.media.MediaBrowserCompat
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
    private suspend fun getRadioStationSaved(radioStationUrl: String): RadioStationLocal? =
        mainRadioStationRepository.getRadioStationSaved(radioStationUrl)

    override suspend fun mediaItemChildrenToRadioStationPresentation(children: MutableList<MediaBrowserCompat.MediaItem>) =
        children.map {
            // Ищем, может такая радиостанция уже сохранена в Room
            val radioStation = getRadioStationSaved(it.description.mediaUri.toString())

            RadioStationPresentation(
                stationName = it.description.title.toString(),
                url = it.mediaId!!,
                urlResolved = it.description.mediaUri.toString(),
                clickCount = it.description.extras?.getLong("ClickCount")
                    ?.toInt()
                    ?: 0,
                countryCode = it.description.subtitle.toString(),
//                country = it.description.extras?.getString("CountryCode")
//                    ?: "",
                isStationInFavourite = radioStation?.isStationInFavourite
                    ?: false, // Если станция уже сохранена, узнаём её isStationInFavorite, если нет - false
                isStationInRecommended = radioStation?.isStationInRecommended
                    ?: false // Если станция уже сохранена, узнаём её isStationInRecommended, если нет - false
            )
        }

    override suspend fun saveLastUsedRadioStationUrlAndCode(url: String, countryCode: String) {
        mainRadioStationRepository.saveLastUsedRadioStationUrlAndCode(url, countryCode)
    }
}