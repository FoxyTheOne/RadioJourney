package com.myproject.radiojourney.presentation.content.radioStationList.favourite

import android.accounts.AccountsException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.favouriteListUseCase.IFavouriteListUseCase
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import com.myproject.radiojourney.other.Event
import com.myproject.radiojourney.other.Resource
import com.myproject.radiojourney.utils.extension.call
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val favouriteListInteractor: IFavouriteListUseCase
) : ViewModel() {

    // Favorite
    private val _radioStationFavouriteListLiveData =
        MutableLiveData<List<RadioStationPresentation>>()
    val radioStationFavouriteListLiveData: MutableLiveData<List<RadioStationPresentation>> =
        _radioStationFavouriteListLiveData

            // <!-- 007 claude
//    private val _stationSavedInFavouritesLiveData = MutableLiveData<Boolean>()
//    val stationSavedInFavouritesLiveData: MutableLiveData<Boolean> =
//        _stationSavedInFavouritesLiveData
//    private val _stationDeletedFromFavouritesLiveData = MutableLiveData<Boolean>()
//    val stationDeletedFromFavouritesLiveData: MutableLiveData<Boolean> =
//        _stationDeletedFromFavouritesLiveData

    // Звезду нажали в этом списке и изменение сохранено в базе. Внутри - станция с новым значением isStationInFavourite.
    // Event - чтобы при пересоздании экрана это изменение не обработалось повторно
    private val _stationFavouriteChangedLiveData = MutableLiveData<Event<RadioStationPresentation>>()
    val stationFavouriteChangedLiveData: LiveData<Event<RadioStationPresentation>> =
        _stationFavouriteChangedLiveData

    // Список изменился на месте (поменялась звезда) - нужно перерисовать
    private val _listRedrawLiveData = MutableLiveData<Boolean>()
    val listRedrawLiveData: LiveData<Boolean> = _listRedrawLiveData
    // 007 claude -->

    private val _addingAStationToAnEmptyListLiveData = MutableLiveData<Boolean>()
    val addingAStationToAnEmptyListLiveData: MutableLiveData<Boolean> =
        _addingAStationToAnEmptyListLiveData

    // LiveData, которые будут отвечать за отображение прогресса (кружок)
    private val _showProgressLiveData = MutableLiveData<Boolean>()
    val showProgressLiveData: MutableLiveData<Boolean> = _showProgressLiveData
    private val _hideProgressLiveData = MutableLiveData<Boolean>()
    val hideProgressLiveData: MutableLiveData<Boolean> = _hideProgressLiveData

    // If smth went wrong
    private val _errorMessageLiveData =
        MutableLiveData<Event<Resource<Boolean>>>() // It must be private, so that other classes can't change it
    val errorMessageLiveData: LiveData<Event<Resource<Boolean>>> =
        _errorMessageLiveData // And another LiveData, that equals to previous, so that classes can't change it

    // LiveData для открытия диалогового окна
    private val _dialogInternetTroubleLiveData = MutableLiveData<Boolean>()
    val dialogInternetTroubleLiveData: MutableLiveData<Boolean> = _dialogInternetTroubleLiveData

    fun getRadioStationFavouriteListAndShow() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val radioStationFavouritePresentationList =
                    favouriteListInteractor.getRadioStationFavouriteList(true)
                _radioStationFavouriteListLiveData.postValue(
                    radioStationFavouritePresentationList
                )
            } catch (e1: AccountsException) {
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

    // <!-- 007 claude
//    fun checkIsStationInFavouritesAndChangeTheStar(currentFavouriteRadioStation: RadioStationPresentation) {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//
//                if (currentFavouriteRadioStation.isStationInFavourite) {
//                    // Если станция есть в избранном и нажали на звезду, нужно из избранного удалить и убрать звезду
//                    favouriteListInteractor.deleteStationInRoomFromFavourite(
//                        currentFavouriteRadioStation
//                    )
//                    _stationDeletedFromFavouritesLiveData.call()
//                    // В случае успеха, так же ставим false в объекте текущей радиостанции
//                    _radioStationFavouriteListLiveData.value.apply {
//                        this?.forEach {
//                            if (it.urlResolved == currentFavouriteRadioStation.urlResolved) {
//                                it.isStationInFavourite = false
//                            }
//                        }
//                    }
//                } else {
//                    // Если станции в избранном нет, нужно добавить её в избранное и поставить звезду
//                    favouriteListInteractor.addStationInRoomToFavourites(
//                        currentFavouriteRadioStation
//                    )
//                    _stationSavedInFavouritesLiveData.call()
//                    // В случае успеха, так же ставим true в объекте текущей радиостанции
//                    _radioStationFavouriteListLiveData.value.apply {
//                        this?.forEach {
//                            if (it.urlResolved == currentFavouriteRadioStation.urlResolved) {
//                                it.isStationInFavourite = true
//                            }
//                        }
//                    }
//                }
//
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

//    fun addAStationToFavouriteListIfItIsNotThere(radioStation: RadioStationPresentation) {
//        viewModelScope.launch(Dispatchers.Default) {
//            try {
//
//                val radioStationList = _radioStationFavouriteListLiveData.value?.toMutableList()
//                var newRadioStationList = radioStationList
//                var radioStationIsInList = false
//
//                radioStationList?.let { nonNullRadioStationList ->
//                    nonNullRadioStationList.forEach {
//                        if (it.urlResolved == radioStation.urlResolved) {
//                            radioStationIsInList = true
//                        }
//                    }
//
//                    // Если добавляем первую станцию в пустой список, нужно убрать надпись
//                    if (!radioStationIsInList && radioStationList.isEmpty()) {
//                        _addingAStationToAnEmptyListLiveData.call()
//                    }
//
//                    // Если искомой радиостанции в списке нет, либо список пуст, её нужно добавить
//                    if (!radioStationIsInList) {
//                        nonNullRadioStationList.add(radioStation)
//                        newRadioStationList = nonNullRadioStationList
//                    }
//                }
//
//                newRadioStationList?.let {
//                    _radioStationFavouriteListLiveData.postValue(it)
//                }
//
//            } catch (e: IOException) {
//                e.printStackTrace()
//                _errorMessageLiveData.postValue(
//                    Event(
//                        Resource.error(
//                            "An unknown error occurred",
//                            null
//                        )
//                    )
//                )
//            }
//        }
//    }

    fun checkIsStationInFavouritesAndChangeTheStar(currentFavouriteRadioStation: RadioStationPresentation) {
        viewModelScope.launch(Dispatchers.IO) {
            try {

                val isFavourite = !currentFavouriteRadioStation.isStationInFavourite
                if (isFavourite) {
                    // Если станции в избранном нет, нужно добавить её в избранное и поставить звезду
                    favouriteListInteractor.addStationInRoomToFavourites(currentFavouriteRadioStation)
                } else {
                    // Если станция есть в избранном и нажали на звезду, нужно из избранного удалить и убрать звезду
                    favouriteListInteractor.deleteStationInRoomFromFavourite(currentFavouriteRadioStation)
                }

                withContext(Dispatchers.Main) {
                    // Сначала меняем признак в списке (по stationuuid - по url могли совпасть разные станции), потом сообщаем экрану
                    setFavouriteInList(currentFavouriteRadioStation.stationuuid, isFavourite)
                    _stationFavouriteChangedLiveData.value =
                        Event(currentFavouriteRadioStation.copy(isStationInFavourite = isFavourite))
                }

            } catch (e1: AccountsException) {
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

//    fun changeTheStar(mediaId: String?, isFavourite: Boolean) {
//        viewModelScope.launch(Dispatchers.Default) {
//            try {
//                _radioStationFavouriteListLiveData.value.apply {
//                    this?.forEach {
//                        if (it.stationuuid == mediaId) {
//                            it.isStationInFavourite = isFavourite
//                        }
//                    }
//                }
//            } catch (e: IOException) {
//                e.printStackTrace()
//                _errorMessageLiveData.postValue(
//                    Event(
//                        Resource.error(
//                            "An unknown error occurred",
//                            null
//                        )
//                    )
//                )
//            }
//        }
//    }

    // Звезду нажали в другом месте (плейер, рекомендованные) - отражаем изменение в этом списке. Вызывать в главном потоке
    fun applyFavouriteChangeFromOutside(radioStation: RadioStationPresentation, isFavourite: Boolean) {
        val radioStationList = _radioStationFavouriteListLiveData.value ?: return // список ещё загружается из базы - он придёт уже с изменением

        if (radioStationList.any { it.stationuuid == radioStation.stationuuid }) {
            setFavouriteInList(radioStation.stationuuid, isFavourite)
        } else if (isFavourite) {
            // Станции в списке нет (добавили в избранное в плейере) - добавляем её в конец списка
            if (radioStationList.isEmpty()) {
                // Если добавляем первую станцию в пустой список, нужно убрать надпись
                _addingAStationToAnEmptyListLiveData.value = true
            }
            _radioStationFavouriteListLiveData.value =
                radioStationList + radioStation.copy(isStationInFavourite = true)
        }
    }

    // Меняем признак у станции в списке (станция, убранная из избранного, остаётся в списке с пустой звездой,
    // чтобы её можно было вернуть) и просим перерисовать список
    private fun setFavouriteInList(stationUuid: String, isFavourite: Boolean) {
        _radioStationFavouriteListLiveData.value?.forEach {
            if (it.stationuuid == stationUuid) it.isStationInFavourite = isFavourite
        }
        _listRedrawLiveData.value = true
    }
// 007 claude -->

}