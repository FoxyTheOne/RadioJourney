package com.myproject.radiojourney.domain.changeFavouriteUseCase

import com.myproject.radiojourney.domain.iRepository.IMainRadioStationRepository
import com.myproject.radiojourney.domain.model.RadioStation
import javax.inject.Inject

/**
 * Добавление станции в избранное и удаление из него.
 * Раньше одинаковые методы addStationInRoomToFavourites / deleteStationInRoomFromFavourite были скопированы
 * в HomeRadioUseCase, FavouriteListUseCase и RecommendedListUseCase (в последнем - без удаления суффикса "_FAV")
 */
class ChangeFavouriteUseCase @Inject constructor(
    private val mainRadioStationRepository: IMainRadioStationRepository
) : IChangeFavouriteUseCase {

    override suspend fun setFavourite(radioStation: RadioStation, isFavourite: Boolean) {
        // В базу сохраняем код страны без "_FAV" (станция из плейлиста избранного)
        mainRadioStationRepository.setStationFavourite(
            radioStation.copy(
                countryCode = radioStation.countryCode.removeSuffix("_FAV"),
                isFavourite = isFavourite
            ),
            isFavourite
        )
    }
}