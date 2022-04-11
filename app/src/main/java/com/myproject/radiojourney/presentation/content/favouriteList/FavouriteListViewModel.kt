package com.myproject.radiojourney.presentation.content.favouriteList

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.favouriteList.IFavouriteListInteractor
import com.myproject.radiojourney.domain.logOut.ILogOutInteractor
import com.myproject.radiojourney.model.presentation.RadioStationFavouritePresentation
import com.myproject.radiojourney.utils.extension.call
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * ViewModel. Здесь осуществляется подписка, запрос через корутины. Работает с Interactor
 */
@HiltViewModel
class FavouriteListViewModel @Inject constructor(
    private val logOutInteractor: ILogOutInteractor,
    private val favouriteListInteractor: IFavouriteListInteractor
) : ViewModel() {
    companion object {
        private const val TAG = "FavouriteListViewModel"
    }

    val favouritesFailedLiveData = MutableLiveData<Boolean>()
    val failedLiveData = MutableLiveData<Boolean>()
    val radioStationFavouriteListLiveData =
        MutableLiveData<List<RadioStationFavouritePresentation>>()
    val stationSavedInFavouritesLiveData = MutableLiveData<Boolean>()
    val stationDeletedFromFavouritesLiveData = MutableLiveData<Boolean>()

    // LiveData, которые будут отвечать за отображение прогресса (кружок)
    val showProgressLiveData = MutableLiveData<Boolean>()
    val hideProgressLiveData = MutableLiveData<Boolean>()

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            showProgressLiveData.call()
            logOutInteractor.onLogout()
            hideProgressLiveData.call()
        }
    }

    fun getRadioStationFavouriteListAndShow() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Уточняем Id
                val userCreatorIdInt = favouriteListInteractor.getToken()
                if (userCreatorIdInt != null) {
                    val radioStationFavouritePresentationList =
                        favouriteListInteractor.getRadioStationFavouriteList(userCreatorIdInt)
                    radioStationFavouriteListLiveData.postValue(
                        radioStationFavouritePresentationList
                    )
                } else {
                    favouritesFailedLiveData.call()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                failedLiveData.call()
            }
        }
    }

    fun checkIsStationInFavouritesAndChangeTheStar(currentFavouriteRadioStation: RadioStationFavouritePresentation) {
        viewModelScope.launch(Dispatchers.IO) {
            // Уточняем Id
            val userCreatorIdInt = favouriteListInteractor.getToken()
            if (userCreatorIdInt != null) {
                // Проверяем, есть ли станция в избранном.
                val isStationInFavourites =
                    favouriteListInteractor.isStationInFavourites(currentFavouriteRadioStation.url)
                if (isStationInFavourites) {
                    // Если станция есть в избранном и нажали на звезду, нужно из избранного удалить и убрать звезду
                    try {
                        favouriteListInteractor.deleteRadioStationFromFavourite(
                            currentFavouriteRadioStation
                        )
                        stationDeletedFromFavouritesLiveData.call()
                        // В случае успеха, так же ставим false в объекте текущей радиостанции
                        currentFavouriteRadioStation.isStationInFavourite = false
                    } catch (e: IOException) {
                        e.printStackTrace()
                        failedLiveData.call()
                    }
                } else {
                    // Если станции в избранном нет, нужно добавить её в избранное и поставить звезду
                    try {
                        favouriteListInteractor.addStationToFavourites(currentFavouriteRadioStation)
                        stationSavedInFavouritesLiveData.call()
                        // В случае успеха, так же ставим true в объекте текущей радиостанции
                        currentFavouriteRadioStation.isStationInFavourite = true
                    } catch (e: IOException) {
                        e.printStackTrace()
                        failedLiveData.call()
                    }
                }
            } else {
                favouritesFailedLiveData.call()
            }
        }
    }
}