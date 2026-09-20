package com.myproject.radiojourney.domain.changeFavouriteUseCase

import com.myproject.radiojourney.domain.model.RadioStation

interface IChangeFavouriteUseCase {
    // Добавить станцию в избранное (isFavourite = true) или убрать из него (false)
    suspend fun setFavourite(radioStation: RadioStation, isFavourite: Boolean)
}