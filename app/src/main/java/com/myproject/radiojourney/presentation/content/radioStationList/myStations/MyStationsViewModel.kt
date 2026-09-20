package com.myproject.radiojourney.presentation.content.radioStationList.myStations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.myStationsUseCase.IMyStationsUseCase
import com.myproject.radiojourney.presentation.model.RadioStationPresentation
import com.myproject.radiojourney.presentation.model.toPresentation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Presentation layer, ViewModel экрана "Мои радиостанции".
 *
 * Список не хранится здесь копией: он приходит из Room подпиской, поэтому после добавления или удаления
 * станции экран обновляется сам - не нужно ни перезагружать список, ни менять его руками
 */
@HiltViewModel
class MyStationsViewModel @Inject constructor(
    private val myStationsInteractor: IMyStationsUseCase
) : ViewModel() {

    // null - список ещё читается из базы. Пустой список и "ещё не загрузили" - разные вещи:
    // иначе на секунду показалась бы надпись "список пуст"
    val myStations: StateFlow<List<RadioStationPresentation>?> =
        myStationsInteractor.getMyStationList()
            .map { stations -> stations.map { it.toPresentation() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun deleteMyStation(stationUuid: String) {
        viewModelScope.launch {
            myStationsInteractor.deleteMyStation(stationUuid)
        }
    }
}