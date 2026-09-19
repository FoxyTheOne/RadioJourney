package com.myproject.radiojourney.domain.changeFavouriteUseCase

import com.myproject.radiojourney.entities.presentation.RadioStationPresentation

interface IChangeFavouriteUseCase {
    // Добавить станцию в избранное (isFavourite = true) или убрать из него (false)
    suspend fun setFavourite(radioStation: RadioStationPresentation, isFavourite: Boolean)
}
