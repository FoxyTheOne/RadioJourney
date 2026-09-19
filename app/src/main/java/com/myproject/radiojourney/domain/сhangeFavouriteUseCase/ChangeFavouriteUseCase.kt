package com.myproject.radiojourney.domain.changeFavouriteUseCase

import com.myproject.radiojourney.domain.iRepository.IMainRadioStationRepository
import com.myproject.radiojourney.entities.local.RadioStationLocal
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import javax.inject.Inject

/**
 * Добавление станции в избранное и удаление из него.
 * Раньше одинаковые методы addStationInRoomToFavourites / deleteStationInRoomFromFavourite были скопированы
 * в HomeRadioUseCase, FavouriteListUseCase и RecommendedListUseCase (в последнем - без удаления суффикса "_FAV")
 */
class ChangeFavouriteUseCase @Inject constructor(
    private val mainRadioStationRepository: IMainRadioStationRepository
) : IChangeFavouriteUseCase {
    override suspend fun setFavourite(
        radioStation: RadioStationPresentation,
        isFavourite: Boolean
    ) {
        // В базу сохраняем код страны без "_FAV" (станция из плейлиста избранного). Делаем копию, а не меняем countryCode
        // у самого объекта станции: иначе станция в плейлисте избранного переставала считаться станцией из избранного
        val radioStationLocal = RadioStationLocal.fromPresentationToLocal(
            radioStation.copy(countryCode = radioStation.countryCode.removeSuffix("_FAV")),
            isStationInFavourite = isFavourite
        )
        mainRadioStationRepository.setStationFavourite(radioStationLocal, isFavourite)
    }
}
