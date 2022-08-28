package com.myproject.radiojourney.presentation.content.homeRadio

import android.accounts.AccountsException
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.homeRadioUseCase.IHomeRadioUseCase
import com.myproject.radiojourney.domain.logOutUseCase.ILogOutUseCase
import com.myproject.radiojourney.entities.presentation.CountryPresentation
import com.myproject.radiojourney.utils.extension.call
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import com.myproject.radiojourney.other.Event
import com.myproject.radiojourney.other.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
class HomeRadioViewModel @Inject constructor(
    private val logOutInteractor: ILogOutUseCase,
    private val homeRadioInteractor: IHomeRadioUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "HomeRadioViewModel"
    }

    // Подписка на локальную БД
    private val _countryListFlow = homeRadioInteractor.subscribeOnCountryList()
    val countryListFlow: Flow<List<CountryPresentation>> = _countryListFlow

    // LiveData, которые будут отвечать за отображение прогресса (кружок)
    private val _showProgressLiveData = MutableLiveData<Boolean>()
    val showProgressLiveData: MutableLiveData<Boolean> = _showProgressLiveData
    private val _hideProgressLiveData = MutableLiveData<Boolean>()
    val hideProgressLiveData: MutableLiveData<Boolean> = _hideProgressLiveData

    // Favourites
    private val _stationSavedInFavouritesLiveData = MutableLiveData<Boolean>()
    val stationSavedInFavouritesLiveData: MutableLiveData<Boolean> =
        _stationSavedInFavouritesLiveData
    private val _stationDeletedFromFavouritesLiveData = MutableLiveData<Boolean>()
    val stationDeletedFromFavouritesLiveData: MutableLiveData<Boolean> =
        _stationDeletedFromFavouritesLiveData
    private val _setTheRightStateOfFavouriteLiveData = MutableLiveData<List<Any>>()
    val setTheRightStateOfFavouriteLiveData: LiveData<List<Any>> =
        _setTheRightStateOfFavouriteLiveData

    // If smth went wrong
    private val _errorMessageLiveData =
        MutableLiveData<Event<Resource<Boolean>>>() // It must be private, so that other classes can't change it
    val errorMessageLiveData: LiveData<Event<Resource<Boolean>>> =
        _errorMessageLiveData // And another LiveData, that equals to previous, so that classes can't change it

    // LiveData для открытия диалогового окна
    private val _dialogInternetTroubleLiveData = MutableLiveData<Boolean>()
    val dialogInternetTroubleLiveData: MutableLiveData<Boolean> = _dialogInternetTroubleLiveData

    // Recommended - проверено работают
    private val eeRockFMEstonia = "https://edge02.cdn.bitflip.ee:8888/rck?_i=5f5ab186"
    private val eeRetroFMEstonia = "https://edge02.cdn.bitflip.ee:8888/RETRO?_i=258f436b"
    private val ltEasyFMURL = "https://netradio.ziniur.lt/easyfm.mp3"
    private val plAntyradioURL =
        "https://n-4-2.dcs.redcdn.pl/sc/o2/Eurozet/live/antyradio.livx?audio=5"
    private val plNnowySwiat = "https://stream.nowyswiat.online/mp3"
    private val plMeloradioAcoustic = "https://ml.cdn.eurozet.pl/MELACO.mp3"
    private val mdVocalTranceRadioDeepVocalHouse = "http://176.9.36.203:8000/deep_320"
    private val roRo90s3NeRgYURL = "https://s11.ssl-stream.com/ssl/90s_energy?mp=/stream"
    private val skBestFM = "http://stream.bestfm.sk/128.mp3"
    private val us2000FMHardRock = "http://bigrradio.cdnstream1.com/5104_128"

    private val recommendedList =
        mapOf(
            eeRockFMEstonia to "EE", eeRetroFMEstonia to "EE",
            ltEasyFMURL to "LT",
            plAntyradioURL to "PL", plNnowySwiat to "PL", plMeloradioAcoustic to "PL",
            mdVocalTranceRadioDeepVocalHouse to "MD",
            roRo90s3NeRgYURL to "RO",
            skBestFM to "SK",
            us2000FMHardRock to "US"
        )

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            _showProgressLiveData.call()
            logOutInteractor.onLogout()
            _hideProgressLiveData.call()
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
                    // Находим в Room по этому URL
                    val radioStationSaved: RadioStationPresentation? =
                        homeRadioInteractor.getRadioStationSaved(radioStationUrl)
                    // Если всё ок - подгружаем
                    radioStationSaved?.let { nonNullRadioStation ->
                        Log.d(
                            TAG,
                            "Передаются значения в LiveData: nonNullRadioStation = $nonNullRadioStation"
                        )
//                        radioStationSavedLiveData.postValue(nonNullRadioStation)
                        // TODO Пересмотреть метод. Возможно, это всё нужно было для старого плейера.Но проставление звездочки favourites нужно и сейчас
                        // Favourites
                        if (nonNullRadioStation.isStationInFavourite) {
                            _stationSavedInFavouritesLiveData.call()
                        }
                    }
                }
            } catch (e1: AccountsException) {
                e1.printStackTrace()
                _dialogInternetTroubleLiveData.call()
            } catch (e2: IOException) {
                e2.printStackTrace()
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

    fun setRecommendedRadioStations() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                homeRadioInteractor.setRecommendedRadioStations(recommendedList)
            } catch (e1: AccountsException) {
                // AccountsException -> Known direct subclasses: AuthenticatorException, NetworkErrorException, OperationCanceledException
                e1.printStackTrace()
                _dialogInternetTroubleLiveData.call()
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

//    fun checkIsStationInFavouritesAndChangeTheStar(currentRadioStation: RadioStationPresentation) {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                // Проверяем, есть ли станция в избранном
//                if (currentRadioStation.isStationInFavourite) {
//                    // Если станция есть в избранном и нажали на звезду, нужно из избранного удалить и убрать звезду
//                    homeRadioInteractor.deleteStationInRoomFromFavourite(currentRadioStation) // Меняем isStationInFavourite = false в Room для последующих обращений к БД
//                    _stationDeletedFromFavouritesLiveData.call()
//                    // В случае успеха, так же ставим false в объекте текущей радиостанции
////                    radioStationSavedLiveData.value.apply {
////                        this?.isStationInFavourite = false
////                    }
//                    // Информацию нужно передать в Activity, т.к. список текущих радиостанций находится там, в SwipeAdapter
//                    _setTheRightStateOfFavouriteLiveData.postValue(
//                        listOf(
//                            currentRadioStation.url,
//                            false
//                        )
//                    )
//                } else {
//                    // Если станции в избранном нет, нужно добавить её в избранное и поставить звезду
//                    homeRadioInteractor.addStationInRoomToFavourites(currentRadioStation) // Меняем isStationInFavourite = true в Room для последующих обращений к БД
//                    _stationSavedInFavouritesLiveData.call()
//                    // В случае успеха, так же ставим true в объекте текущей радиостанции
////                    radioStationSavedLiveData.value.apply {
////                        this?.isStationInFavourite = true
////                    }
//                    // Информацию нужно передать в Activity, т.к. список текущих радиостанций находится там, в SwipeAdapter
//                    _setTheRightStateOfFavouriteLiveData.postValue(
//                        listOf(
//                            currentRadioStation.url,
//                            true
//                        )
//                    )
//                }
//            } catch (e1: AccountsException) {
//                e1.printStackTrace()
//                _dialogInternetTroubleLiveData.call()
//            } catch (e: IOException) {
//                e.printStackTrace()
//                _errorMessageLiveData.postValue(
//                    Event(
//                        Resource.error(
//                            "Failed connecting to the local database",
//                            null
//                        )
//                    )
//                )
//            }
//        }
//    }

}
