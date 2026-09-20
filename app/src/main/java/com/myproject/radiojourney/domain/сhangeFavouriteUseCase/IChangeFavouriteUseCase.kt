package com.myproject.radiojourney.domain.changeFavouriteUseCase

import com.myproject.radiojourney.domain.model.RadioStation

/**
 * UseCase "добавить станцию в избранное или убрать из него".
 *
 * Отдельный интерфейс, потому что звезду нажимают на трёх экранах: в плеере, в списке избранного и в списке станций.
 * Раньше одна и та же работа с базой была написана в каждой из этих ViewModel
 */
interface IChangeFavouriteUseCase {
    // Добавить станцию в избранное (isFavourite = true) или убрать из него (false)
    suspend fun setFavourite(radioStation: RadioStation, isFavourite: Boolean)
}