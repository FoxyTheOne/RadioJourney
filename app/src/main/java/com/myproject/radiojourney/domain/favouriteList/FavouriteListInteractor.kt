package com.myproject.radiojourney.domain.favouriteList

import android.util.Log
import com.myproject.radiojourney.domain.iRepository.IContentRepository
import com.myproject.radiojourney.model.local.RadioStationFavouriteLocal
import com.myproject.radiojourney.model.presentation.RadioStationFavouritePresentation
import javax.inject.Inject

/**
 * Interactor. Domain layer. Работает только с Repository.
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor)
 * При работе с model, здесь происходит преобразование local -> presentation (опционально)
 */
class FavouriteListInteractor @Inject constructor(
    private val contentRepository: IContentRepository
) : IFavouriteListInteractor {
    companion object {
        private const val TAG = "FavouriteListInteractor"
    }

    override suspend fun getToken(): Int? = contentRepository.getToken()

    override suspend fun getRadioStationFavouriteList(userCreatorIdInt: Int): List<RadioStationFavouritePresentation> {
        // Получаем из репозитория List<UserWithStations>
        val userWithStationsList = contentRepository.getUsersWithStations()

        // local -> presentation: выбираем Список только нашего User и передаём обратно
        var radioStationFavouriteList = listOf<RadioStationFavouriteLocal>()
        userWithStationsList.forEach { userWithStations ->
            Log.d(TAG, "результат userWithStationsList: $userWithStations")
            if (userWithStations.user.id == userCreatorIdInt) {
                radioStationFavouriteList = userWithStations.stations
            }
        }

        val radioStationFavouritePresentationList =
            mutableListOf<RadioStationFavouritePresentation>()
        radioStationFavouriteList.forEach { radioStationFavouriteLocal ->
            val radioStationFavouritePresentation =
                RadioStationFavouritePresentation.fromLocalToPresentation(radioStationFavouriteLocal)
            radioStationFavouritePresentationList.add(radioStationFavouritePresentation)
        }

        return radioStationFavouritePresentationList
    }

    override suspend fun isStationInFavourites(url: String): Boolean =
        contentRepository.isStationInFavourites(url)

    override suspend fun deleteRadioStationFromFavourite(currentFavouriteRadioStation: RadioStationFavouritePresentation) {
        val currentRadioStationFavouriteLocal =
            RadioStationFavouriteLocal.fromFavouritePresentationToFavouriteLocal(
                currentFavouriteRadioStation,
                isStationInFavourite = false
            )
        contentRepository.deleteRadioStationFromFavourite(currentRadioStationFavouriteLocal)
    }

    override suspend fun addStationToFavourites(currentFavouriteRadioStation: RadioStationFavouritePresentation) {
        val currentRadioStationFavouriteLocal =
            RadioStationFavouriteLocal.fromFavouritePresentationToFavouriteLocal(
                currentFavouriteRadioStation,
                isStationInFavourite = true
            )
        contentRepository.addStationToFavourites(currentRadioStationFavouriteLocal)
    }
}