package com.myproject.radiojourney.presentation.content.radioStationList.radioList

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.radioListUseCase.IRadioListUseCase
import com.myproject.radiojourney.other.ServerError
import com.myproject.radiojourney.other.Status
import com.myproject.radiojourney.presentation.model.RadioStationPresentation
import com.myproject.radiojourney.presentation.model.toPresentation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel экрана со списком станций одной страны.
 *
 * Состояние экрана - одно поле uiState (загрузка / список / сервер недоступен): так экран не может показать
 * одновременно список и ошибку. Код и название страны ViewModel берёт из аргументов навигации сама (SavedStateHandle),
 * а список запрашивает один раз при создании - при возвращении на экран кнопкой "назад" он не скачивается заново
 */
@HiltViewModel
class RadioListViewModel @Inject constructor(
    private val radioListInteractor: IRadioListUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Состояние экрана: список загружается, загружен или сервер недоступен
    sealed interface UiState {
        data object Loading : UiState

        // Список запасной (см. RadioStationList): savedAt - сервер не ответил, и показан список, сохранённый в это время;
        // isOnlyPopular - полный список обрывался, и показаны только самые популярные станции. Обычный список - null и false
        data class Loaded(
            val radioStations: List<RadioStationPresentation>,
            val savedAt: Long? = null,
            val isOnlyPopular: Boolean = false
        ) : UiState

        data class ServerIsDown(val reason: ServerError) : UiState
    }

    // Аргументы навигации (код и название страны) ViewModel получает сама через SavedStateHandle
    val countryCode: String = savedStateHandle["country_code"] ?: ""
    val countryName: String = savedStateHandle["country_name"] ?: ""
//    Это «сумка» с данными, которую Hilt отдаёт ViewModel при создании. В неё автоматически складываются аргументы навигации (те, что описаны в app_navigation.xml для этого экрана).
//    Поэтому RadioListViewModel берёт код и название страны сам:
//    val countryCode: String = savedStateHandle["country_code"] ?: ""
//    Зачем так, а не передавать из фрагмента: во-первых, ViewModel может начать загрузку сразу при создании, не дожидаясь, пока фрагмент передаст ей аргументы;
//    во-вторых, содержимое SavedStateHandle переживает не только поворот экрана, но и «смерть процесса» — когда система выгружает приложение из памяти, а пользователь потом возвращается в него из списка недавних.
//    Обычные поля ViewModel в этом случае теряются, а туда можно и свои значения класть (savedStateHandle["key"] = value).

    // Раньше - две LiveData (список и "сервер недоступен"). Одно состояние не даёт экрану показать оба сразу
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Список загружается один раз при создании ViewModel. Раньше загрузка запускалась из onViewCreated,
        // и при каждом возвращении на этот экран (кнопкой "назад") список заново скачивался с сервера
        loadRadioStationList()
    }

    // Получаем список радиостанций, преобразуем. Список всегда запрашивается с сервера: там он постоянно обновляется.
    // Сохранённый в телефоне список (последний удачно скачанный) репозиторий отдаёт, только если сервер недоступен.
    // Сетевой запрос сам выполняется в фоновом потоке (Retrofit suspend), Dispatchers.IO не нужен
    private fun loadRadioStationList() {
        viewModelScope.launch {
            val radioStationResource = radioListInteractor.getRadioStationList(countryCode)
            val radioStationList = radioStationResource.data
            _uiState.value =
                if (radioStationResource.status == Status.ERROR || radioStationList == null) {
                    UiState.ServerIsDown(ServerError.fromMessage(radioStationResource.message))
                } else {
                    UiState.Loaded(
                        radioStationList.stations.map { it.toPresentation() },
                        radioStationList.savedAt,
                        radioStationList.isOnlyPopular
                    )
                }
        }
    }
}