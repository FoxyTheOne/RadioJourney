package com.myproject.radiojourney.presentation.content.homeRadio

import android.accounts.AccountsException
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.homeRadio.IHomeRadioUseCase
import com.myproject.radiojourney.domain.logOut.ILogOutUseCase
import com.myproject.radiojourney.utils.extension.call
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
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
class HomeRadioViewModel @Inject constructor(
    private val logOutInteractor: ILogOutUseCase,
    private val homeRadioInteractor: IHomeRadioUseCase
) : ViewModel() {
    companion object {
        private const val TAG = "HomeRadioViewModel"
    }

    val failedLiveData = MutableLiveData<Boolean>()
    val radioStationSavedLiveData = MutableLiveData<RadioStationPresentation>()

    // Подписка на локальную БД
    val countryListFlow = homeRadioInteractor.subscribeOnCountryList()

    // LiveData, которые будут отвечать за отображение прогресса (кружок)
    val showProgressLiveData = MutableLiveData<Boolean>()
    val hideProgressLiveData = MutableLiveData<Boolean>()

    // LiveData для открытия диалогового окна
    val dialogInternetTroubleLiveData = MutableLiveData<Boolean>()

    // Favourites
    val stationSavedInFavouritesLiveData = MutableLiveData<Boolean>()
    val stationDeletedFromFavouritesLiveData = MutableLiveData<Boolean>()

    // Recommended - проверено работают
    private val eeRockFMEstonia = "https://edge02.cdn.bitflip.ee:8888/rck?_i=5f5ab186"// checked
    private val eeRetroFMEstonia = "https://edge02.cdn.bitflip.ee:8888/RETRO?_i=258f436b"// checked
    private val ltEasyFMURL = "https://netradio.ziniur.lt/easyfm.mp3" // checked
    private val plAntyradioURL =
        "https://n-4-2.dcs.redcdn.pl/sc/o2/Eurozet/live/antyradio.livx?audio=5" // checked
    private val plNnowySwiat = "https://stream.nowyswiat.online/mp3" // checking
    private val plMeloradioAcoustic = "https://ml.cdn.eurozet.pl/MELACO.mp3"
    private val mdVocalTranceRadioDeepVocalHouse = "http://176.9.36.203:8000/deep_320"
    private val roRo90s3NeRgYURL = "https://s11.ssl-stream.com/ssl/90s_energy?mp=/stream"
    private val skBestFM = "http://stream.bestfm.sk/128.mp3" // checked
    private val us2000FMHardRock = "http://bigrradio.cdnstream1.com/5104_128" // checking

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
                    // Находим в Room по этому URL
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
                        if (nonNullRadioStation.isStationInFavourite) {
                            stationSavedInFavouritesLiveData.call()
                        }
                    }
                }
            } catch (e1: AccountsException) {
                e1.printStackTrace()
                dialogInternetTroubleLiveData.call()
            } catch (e2: IOException) {
                e2.printStackTrace()
                failedLiveData.call()
            }
        }
    }

    // Получаем радиостанцию из списка на предыдущей странице, если перешли сюда из списка радиостанций
    fun saveRadioStationAndShow(
        radioStation: RadioStationPresentation,
        isNavigatedFromFavorite: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Поменять в Shared Preference setIsRadioStationStored на true. Сохранить в Shared Preference (url)
                homeRadioInteractor.saveRadioStationUrl(true, radioStation.url)
                // И сохранить радиостанцию в Room. Если перешли сюда из списка любимых радиостанций - они уже сохранены в Room
                if (!isNavigatedFromFavorite) {
                    homeRadioInteractor.saveRadioStationInRoom(radioStation)
                }
                // Отобразить
                radioStationSavedLiveData.postValue(radioStation)
                // Favourites
                if (radioStation.isStationInFavourite) {
                    stationSavedInFavouritesLiveData.call()
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

    fun checkIsStationInFavouritesAndChangeTheStar(currentRadioStation: RadioStationPresentation) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Проверяем, есть ли станция в избранном
                if (currentRadioStation.isStationInFavourite) {
                    // Если станция есть в избранном и нажали на звезду, нужно из избранного удалить и убрать звезду
                    homeRadioInteractor.deleteStationInRoomFromFavourite(currentRadioStation) // Меняем isStationInFavourite = false в Room для последующих обращений к БД
                    stationDeletedFromFavouritesLiveData.call()
                    // В случае успеха, так же ставим false в объекте текущей радиостанции
                    radioStationSavedLiveData.value.apply {
                        this?.isStationInFavourite = false
                    }
                } else {
                    // Если станции в избранном нет, нужно добавить её в избранное и поставить звезду
                    homeRadioInteractor.addStationInRoomToFavourites(currentRadioStation) // Меняем isStationInFavourite = true в Room для последующих обращений к БД
                    stationSavedInFavouritesLiveData.call()
                    // В случае успеха, так же ставим true в объекте текущей радиостанции
                    radioStationSavedLiveData.value.apply {
                        this?.isStationInFavourite = true
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

    fun setRecommendedRadioStations() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                homeRadioInteractor.setRecommendedRadioStations(recommendedList)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
