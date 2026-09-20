package com.myproject.radiojourney.presentation.content.radioStationList.myStations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.myStationsUseCase.IMyStationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel окна "Новая станция".
 *
 * У диалога своя ViewModel, а не общая с экраном списка: диалог должен остаться открытым, если пользователь
 * ошибся в ссылке, и показать ошибку под полем. Для этого ему нужен ответ на свой запрос, а не общее состояние экрана
 */
@HiltViewModel
class AddMyStationViewModel @Inject constructor(
    private val myStationsInteractor: IMyStationsUseCase
) : ViewModel() {

    // Результат попытки сохранить. Channel, а не StateFlow: это событие, его нужно обработать один раз
    // (иначе после поворота экрана ошибка показалась бы снова)
    private val _addResults = Channel<IMyStationsUseCase.AddResult>(Channel.BUFFERED)
    val addResults: Flow<IMyStationsUseCase.AddResult> = _addResults.receiveAsFlow()

    fun addMyStation(name: String, url: String) {
        viewModelScope.launch {
            // Проверка введённого и сохранение - в use case: это правила приложения, а не дело экрана
            _addResults.send(myStationsInteractor.addMyStation(name, url))
        }
    }
}