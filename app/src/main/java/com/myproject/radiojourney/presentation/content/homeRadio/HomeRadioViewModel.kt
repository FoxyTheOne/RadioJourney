package com.myproject.radiojourney.presentation.content.homeRadio

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.homeRadio.IHomeRadioInteractor
import com.myproject.radiojourney.domain.logOut.ILogOutInteractor
import com.myproject.radiojourney.model.presentation.RadioStationFavouritePresentation
import com.myproject.radiojourney.utils.extension.call
import com.myproject.radiojourney.model.presentation.RadioStationPresentation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * ViewModel. Здесь осуществляется подписка, запрос через корутины. Работает с Interactor
 */
@HiltViewModel
class HomeRadioViewModel @Inject constructor(
    private val logOutInteractor: ILogOutInteractor,
    private val homeRadioInteractor: IHomeRadioInteractor
) : ViewModel() {
    companion object {
        private const val TAG = "HomeRadioViewModel"
    }

    val failedLiveData = MutableLiveData<Boolean>()

    // Подписка на локальную БД
    val countryListFlow = homeRadioInteractor.subscribeOnCountryList()

    val radioStationSavedLiveData = MutableLiveData<RadioStationPresentation>()

    // LiveData, которые будут отвечать за отображение прогресса (кружок)
    val showProgressLiveData = MutableLiveData<Boolean>()
    val hideProgressLiveData = MutableLiveData<Boolean>()

    // LiveData для открытия диалогового окна
    val dialogInternetTroubleLiveData = MutableLiveData<Boolean>()

    // Favourites
    val addStationToFavouritesFailedLiveData = MutableLiveData<Boolean>()
    val stationSavedInFavouritesLiveData = MutableLiveData<Boolean>()
    val stationDeletedFromFavouritesLiveData = MutableLiveData<Boolean>()

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            showProgressLiveData.call()
            logOutInteractor.onLogout()
            hideProgressLiveData.call()
        }
    }

    // Подгрузить радиостанцию из Shared Preference, если она там сохранена. Если нет - текст "выберите радиостанцию"
    fun getStoredRadioStation() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Узнаём, была ли ранее сохнанена радиостанция
                val isRadioStationStored = homeRadioInteractor.isRadioStationStored()
                Log.d(
                    TAG,
                    "Узнали, была ли сохранена радиостанция: $isRadioStationStored"
                )
                // Если да - выводим её на экран
                if (isRadioStationStored) {
                    // Узнаём URL
                    val radioStationUrl: String =
                        homeRadioInteractor.getRadioStationUrl().toString()
                    // Находим в Room
                    val radioStationSaved: RadioStationPresentation? =
                        homeRadioInteractor.getRadioStationSaved(radioStationUrl)
                    // Если всё ок - подгружаем
                    radioStationSaved?.let { nonNullRadioStation ->
                        Log.d(
                            TAG,
                            "Передаются значения в LiveData: nonNullRadioStation = $nonNullRadioStation"
                        )
                        radioStationSavedLiveData.postValue(nonNullRadioStation)
                        // Favourites
                        val isStationInFavourites =
                            homeRadioInteractor.isStationInFavourites(nonNullRadioStation.url)
                        if (isStationInFavourites) {
                            stationSavedInFavouritesLiveData.call()
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                failedLiveData.call()
            }
        }
    }

    // Получаем радиостанцию из списка на предыдущей странице, если перешли сюда из списка радиостанций
    fun saveRadioStationAndShow(radioStation: RadioStationPresentation) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Поменять в Shared Preference setIsRadioStationStored на true
                // Сохранить в Shared Preference (url) и Room (станцию)
                homeRadioInteractor.saveRadioStationUrl(true, radioStation)
                // Отобразить
                radioStationSavedLiveData.postValue(radioStation)
                // Favourites
                val isStationInFavourites =
                    homeRadioInteractor.isStationInFavourites(radioStation.url)
                if (isStationInFavourites) {
                    stationSavedInFavouritesLiveData.call()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                failedLiveData.call()
            }
        }
    }

    // Получаем радиостанцию из списка на предыдущей странице, если перешли сюда из списка любимых радиостанций (они уже сохранены в Room)
    fun saveFavouriteRadioStationAndShow(radioStationFavourite: RadioStationFavouritePresentation) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Поменять в Shared Preference setIsRadioStationStored на true
                // Сохранить в Shared Preference (url) (сохранять в Room не нужно, она уже там)
                homeRadioInteractor.saveFavouriteRadioStationUrl(true, radioStationFavourite.url)
                // Отобразить
                val radioStationPresentation =
                    RadioStationPresentation.fromFavouritePresentationToPresentation(
                        radioStationFavourite
                    )
                radioStationSavedLiveData.postValue(radioStationPresentation)
                // Favourites (проверку на всякий случай оставляю. Вдруг пользователь уберет звезду (удалит из избранного), а потом нажмёт на станцию и перейдет в этот фрагмент её слушать)
                val isStationInFavourites =
                    homeRadioInteractor.isStationInFavourites(radioStationFavourite.url)
                if (isStationInFavourites) {
                    stationSavedInFavouritesLiveData.call()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                failedLiveData.call()
            }
        }
    }

    fun addStationToFavourites(currentRadioStation: RadioStationPresentation) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userCreatorIdInt = homeRadioInteractor.getToken()
                if (userCreatorIdInt != null) {
                    homeRadioInteractor.addStationToFavourites(
                        userCreatorIdInt,
                        currentRadioStation
                    )
                    stationSavedInFavouritesLiveData.call()
                    // В случае успеха, так же ставим true в объекте текущей радиостанции
                    currentRadioStation.isStationInFavourite = true
                } else {
                    addStationToFavouritesFailedLiveData.call()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                failedLiveData.call()
            }
        }
    }

    fun checkIsStationInFavouritesAndChangeTheStar(currentRadioStation: RadioStationPresentation) {
        viewModelScope.launch(Dispatchers.IO) {
            // Уточняем Id
            val userCreatorIdInt = homeRadioInteractor.getToken()
            if (userCreatorIdInt != null) {
                // Проверяем, есть ли станция в избранном
                val isStationInFavourites =
                    homeRadioInteractor.isStationInFavourites(currentRadioStation.url)
                if (isStationInFavourites) {
                    // Если станция есть в избранном и нажали на звезду, нужно из избранного удалить и убрать звезду
                    try {
                        homeRadioInteractor.deleteRadioStationFromFavourite(
                            userCreatorIdInt,
                            currentRadioStation
                        )
                        stationDeletedFromFavouritesLiveData.call()
                        // В случае успеха, так же ставим false в объекте текущей радиостанции
                        currentRadioStation.isStationInFavourite = false
                    } catch (e: IOException) {
                        e.printStackTrace()
                        failedLiveData.call()
                    }
                } else {
                    // Если станции в избранном нет, нужно добавить её в избранное и поставить звезду
                    try {
                        homeRadioInteractor.addStationToFavourites(
                            userCreatorIdInt,
                            currentRadioStation
                        )
                        stationSavedInFavouritesLiveData.call()
                        // В случае успеха, так же ставим true в объекте текущей радиостанции
                        currentRadioStation.isStationInFavourite = true
                    } catch (e: IOException) {
                        e.printStackTrace()
                        failedLiveData.call()
                    }
                }
            } else {
                addStationToFavouritesFailedLiveData.call()
            }
        }
    }
}