package com.myproject.radiojourney.presentation.content.radioStationList.favourite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.changeFavouriteUseCase.IChangeFavouriteUseCase
import com.myproject.radiojourney.domain.favouriteListUseCase.IFavouriteListUseCase
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import com.myproject.radiojourney.other.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Presentation layer, ViewModel. Работа с компонентами Android. Работает только с Interactor.
 *
 * Interactor - объект, который реализует UseCase, используя бизнес-объекты Entities.
 * Здесь осуществляется подписка, запрос через корутины.
 */
@HiltViewModel
class FavouriteListViewModel @Inject constructor(
    private val favouriteListInteractor: IFavouriteListUseCase,
    private val changeFavouriteInteractor: IChangeFavouriteUseCase
) : ViewModel() {

    // Список избранного. Звезда меняется у станции в списке; убранная из избранного станция остаётся в списке без звезды,
    // чтобы её можно было вернуть (из списка она пропадёт при следующем открытии экрана)
    private val _radioStationFavouriteListLiveData =
        MutableLiveData<List<RadioStationPresentation>>()
    val radioStationFavouriteListLiveData: LiveData<List<RadioStationPresentation>> =
        _radioStationFavouriteListLiveData

    // Звезду нажали в этом списке - экран сообщит плейеру (MainViewModel.notifyFavouriteChanged), какая станция изменилась
    private val _stationFavouriteChangedLiveData =
        MutableLiveData<Event<RadioStationPresentation>>()
    val stationFavouriteChangedLiveData: LiveData<Event<RadioStationPresentation>> =
        _stationFavouriteChangedLiveData

    init {
        // Room сам выполняет suspend-запросы в фоновом потоке, отдельный Dispatchers.IO не нужен
        viewModelScope.launch {
            _radioStationFavouriteListLiveData.value =
                favouriteListInteractor.getRadioStationFavouriteList()
        }
    }

    fun checkIsStationInFavouritesAndChangeTheStar(radioStation: RadioStationPresentation) {
        viewModelScope.launch {
            val isFavourite = !radioStation.isStationInFavourite
            changeFavouriteInteractor.setFavourite(radioStation, isFavourite)
            setFavouriteInList(radioStation.stationuuid, isFavourite)
            _stationFavouriteChangedLiveData.value =
                Event(radioStation.copy(isStationInFavourite = isFavourite))
        }
    }

    // Звезду нажали в плейере, пока открыт этот список: меняем звезду у той же станции или добавляем станцию в список
    fun applyFavouriteChangeFromOutside(
        radioStation: RadioStationPresentation,
        isFavourite: Boolean
    ) {
        val radioStationList = _radioStationFavouriteListLiveData.value
            ?: return // список ещё загружается из базы - он придёт уже с изменением
        if (radioStationList.any { it.stationuuid == radioStation.stationuuid }) {
            setFavouriteInList(radioStation.stationuuid, isFavourite)
        } else if (isFavourite) {
            _radioStationFavouriteListLiveData.value =
                radioStationList + radioStation.copy(isStationInFavourite = true)
        }
    }

    // Новый список с изменённой станцией (а не изменение объекта в старом списке) - LiveData сообщит экрану об изменении
    private fun setFavouriteInList(stationUuid: String, isFavourite: Boolean) {
        _radioStationFavouriteListLiveData.value = _radioStationFavouriteListLiveData.value?.map {
            if (it.stationuuid == stationUuid) it.copy(isStationInFavourite = isFavourite) else it
        }
    }
}