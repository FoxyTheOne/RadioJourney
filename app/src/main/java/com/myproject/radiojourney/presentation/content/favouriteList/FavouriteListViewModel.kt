package com.myproject.radiojourney.presentation.content.favouriteList

import android.accounts.AccountsException
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.favouriteList.IFavouriteListUseCase
import com.myproject.radiojourney.domain.logOut.ILogOutUseCase
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import com.myproject.radiojourney.utils.extension.call
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * Presentation layer, ViewModel. Работа с компонентами Android. Работает только с Interactor.
 *
 * Interactor - объект, который реализует UseCase, используя бизнес-объекты Entities.
 * Здесь осуществляется подписка, запрос через корутины.
 */
@HiltViewModel
class FavouriteListViewModel @Inject constructor(
    private val logOutInteractor: ILogOutUseCase,
    private val favouriteListInteractor: IFavouriteListUseCase
) : ViewModel() {

    val failedLiveData = MutableLiveData<Boolean>()

    // LiveData для открытия диалогового окна
    val dialogInternetTroubleLiveData = MutableLiveData<Boolean>()

    // Favorite
    val radioStationFavouriteListLiveData =
        MutableLiveData<List<RadioStationPresentation>>()
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
                val radioStationFavouritePresentationList =
                    favouriteListInteractor.getRadioStationFavouriteList(true)
                radioStationFavouriteListLiveData.postValue(
                    radioStationFavouritePresentationList
                )
            } catch (e1: AccountsException) {
                e1.printStackTrace()
                dialogInternetTroubleLiveData.call()
            } catch (e: IOException) {
                e.printStackTrace()
                failedLiveData.call()
            }
        }
    }

    fun checkIsStationInFavouritesAndChangeTheStar(currentFavouriteRadioStation: RadioStationPresentation) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (currentFavouriteRadioStation.isStationInFavourite) {
                    // Если станция есть в избранном и нажали на звезду, нужно из избранного удалить и убрать звезду
                    favouriteListInteractor.deleteStationInRoomFromFavourite(
                        currentFavouriteRadioStation
                    )
                    stationDeletedFromFavouritesLiveData.call()
                    // В случае успеха, так же ставим false в объекте текущей радиостанции
                    radioStationFavouriteListLiveData.value.apply {
                        this?.forEach {
                            if (it.url == currentFavouriteRadioStation.url) {
                                it.isStationInFavourite = false
                            }
                        }
                    }
                } else {
                    // Если станции в избранном нет, нужно добавить её в избранное и поставить звезду
                    favouriteListInteractor.addStationInRoomToFavourites(
                        currentFavouriteRadioStation
                    )
                    stationSavedInFavouritesLiveData.call()
                    // В случае успеха, так же ставим true в объекте текущей радиостанции
                    radioStationFavouriteListLiveData.value.apply {
                        this?.forEach {
                            if (it.url == currentFavouriteRadioStation.url) {
                                it.isStationInFavourite = true
                            }
                        }
                    }
                }
            } catch (e1: AccountsException) {
                e1.printStackTrace()
                dialogInternetTroubleLiveData.call()
            } catch (e: IOException) {
                e.printStackTrace()
                failedLiveData.call()
            }
        }
    }

}