package com.myproject.radiojourney.presentation.content.radioStationList.favourite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.changeFavouriteUseCase.IChangeFavouriteUseCase
import com.myproject.radiojourney.domain.favouriteListUseCase.IFavouriteListUseCase
import com.myproject.radiojourney.presentation.model.RadioStationPresentation
import com.myproject.radiojourney.presentation.model.toDomain
import com.myproject.radiojourney.presentation.model.toPresentation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Presentation layer, ViewModel. Работа с компонентами Android. Работает только с UseCase.
 * Здесь осуществляется подписка, запрос через корутины.
 */
@HiltViewModel
class FavouriteListViewModel @Inject constructor(
    private val favouriteListInteractor: IFavouriteListUseCase,
    private val changeFavouriteInteractor: IChangeFavouriteUseCase
) : ViewModel() {

    // Список избранного. Звезда меняется у станции в списке; убранная из избранного станция остаётся в списке без звезды,
    // чтобы её можно было вернуть (из списка она пропадёт при следующем открытии экрана). null - список ещё загружается
    private val _radioStationFavouriteList = MutableStateFlow<List<RadioStationPresentation>?>(null)
    val radioStationFavouriteList: StateFlow<List<RadioStationPresentation>?> = _radioStationFavouriteList.asStateFlow()

    // Звезду нажали в этом списке - экран сообщит плейеру (MainViewModel.notifyFavouriteChanged), какая станция изменилась
    private val _stationFavouriteChanged = MutableSharedFlow<RadioStationPresentation>(extraBufferCapacity = 8)
    val stationFavouriteChanged: SharedFlow<RadioStationPresentation> = _stationFavouriteChanged.asSharedFlow()

    init {
        // Room сам выполняет suspend-запросы в фоновом потоке, отдельный Dispatchers.IO не нужен
        viewModelScope.launch {
            _radioStationFavouriteList.value = favouriteListInteractor.getRadioStationFavouriteList().map { it.toPresentation() }
        }
    }

    fun checkIsStationInFavouritesAndChangeTheStar(radioStation: RadioStationPresentation) {
        viewModelScope.launch {
            val isFavourite = !radioStation.isStationInFavourite
            changeFavouriteInteractor.setFavourite(radioStation.toDomain(), isFavourite)
            setFavouriteInList(radioStation.stationuuid, isFavourite)
            _stationFavouriteChanged.emit(radioStation.copy(isStationInFavourite = isFavourite))
        }
    }

    // Звезду нажали в плейере, пока открыт этот список: меняем звезду у той же станции или добавляем станцию в список
    fun applyFavouriteChangeFromOutside(radioStation: RadioStationPresentation, isFavourite: Boolean) {
        _radioStationFavouriteList.update { radioStationList ->
            when {
                radioStationList == null -> null // список ещё загружается из базы - он придёт уже с изменением
                radioStationList.any { it.stationuuid == radioStation.stationuuid } -> radioStationList.withFavourite(radioStation.stationuuid, isFavourite)
                isFavourite -> radioStationList + radioStation.copy(isStationInFavourite = true)
                else -> radioStationList
            }
        }
    }

    // Новый список с изменённой станцией (а не изменение объекта в старом списке) - StateFlow сообщит экрану об изменении
    private fun setFavouriteInList(stationUuid: String, isFavourite: Boolean) {
        _radioStationFavouriteList.update { it?.withFavourite(stationUuid, isFavourite) }
    }

    private fun List<RadioStationPresentation>.withFavourite(stationUuid: String, isFavourite: Boolean) =
        map { if (it.stationuuid == stationUuid) it.copy(isStationInFavourite = isFavourite) else it }
}