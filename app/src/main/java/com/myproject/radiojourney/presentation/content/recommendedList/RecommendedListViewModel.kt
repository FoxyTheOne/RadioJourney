package com.myproject.radiojourney.presentation.content.recommendedList

import android.accounts.AccountsException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.logOutUseCase.ILogOutUseCase
import com.myproject.radiojourney.domain.recommendedListUseCase.IRecommendedListUseCase
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import com.myproject.radiojourney.other.Event
import com.myproject.radiojourney.other.Resource
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
class RecommendedListViewModel @Inject constructor(
    private val logOutInteractor: ILogOutUseCase,
    private val recommendedListInteractor: IRecommendedListUseCase
) : ViewModel() {
    private val _errorMessageLiveData =
        MutableLiveData<Event<Resource<Boolean>>>() // It must be private, so that other classes can't change it
    val errorMessageLiveData: LiveData<Event<Resource<Boolean>>> =
        _errorMessageLiveData // And another LiveData, that equals to previous, so that classes can't change it

    val radioStationRecommendedListLiveData = MutableLiveData<List<RadioStationPresentation>>()

    // LiveData для открытия диалогового окна
    val dialogInternetTroubleLiveData = MutableLiveData<Boolean>()

    // Favorite
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

    fun getRadioStationRecommendedListAndShow() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val radioStationRecommendedList =
                    recommendedListInteractor.getRadioStationRecommendedList()
                radioStationRecommendedListLiveData.postValue(
                    radioStationRecommendedList
                )
            } catch (e1: AccountsException) {
                e1.printStackTrace()
                dialogInternetTroubleLiveData.call()
            } catch (e: IOException) {
                e.printStackTrace()
                _errorMessageLiveData.postValue(
                    Event(
                        Resource.error(
                            "Failed connecting to the local database",
                            null
                        )
                    )
                )
            }
        }
    }

    fun checkIsStationInFavouritesAndChangeTheStar(radioStationOnStarClick: RadioStationPresentation) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (radioStationOnStarClick.isStationInFavourite) {
                    // Если станция есть в избранном и нажали на звезду, нужно из избранного удалить и убрать звезду
                    recommendedListInteractor.deleteStationInRoomFromFavourite(
                        radioStationOnStarClick
                    )
                    stationDeletedFromFavouritesLiveData.call()
                    // В случае успеха, так же ставим false в объекте текущей радиостанции
                    radioStationRecommendedListLiveData.value.apply {
                        this?.forEach {
                            if (it.url == radioStationOnStarClick.url) {
                                it.isStationInFavourite = false
                            }
                        }
                    }
                } else {
                    // Если станции в избранном нет, нужно добавить её в избранное и поставить звезду
                    recommendedListInteractor.addStationInRoomToFavourites(
                        radioStationOnStarClick
                    )
                    stationSavedInFavouritesLiveData.call()
                    // В случае успеха, так же ставим true в объекте текущей радиостанции
                    radioStationRecommendedListLiveData.value.apply {
                        this?.forEach {
                            if (it.url == radioStationOnStarClick.url) {
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
                _errorMessageLiveData.postValue(
                    Event(
                        Resource.error(
                            "Failed connecting to the local database",
                            null
                        )
                    )
                )
            }
        }
    }

}