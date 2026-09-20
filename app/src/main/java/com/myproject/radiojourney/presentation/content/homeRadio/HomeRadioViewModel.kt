package com.myproject.radiojourney.presentation.content.homeRadio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.homeRadioUseCase.IHomeRadioUseCase
import com.myproject.radiojourney.presentation.model.CountryPresentation
import com.myproject.radiojourney.presentation.model.toPresentation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Presentation layer, ViewModel. Работа с компонентами Android. Работает только с UseCase.
 * Здесь осуществляется подписка, запрос через корутины.
 */
@HiltViewModel
class HomeRadioViewModel @Inject constructor(
    private val homeRadioInteractor: IHomeRadioUseCase
) : ViewModel() {

    // Скрыт ли информационный блок. null - значение ещё не прочитано из настроек
    private val _isInfoHidden = MutableStateFlow<Boolean?>(null)
    val isInfoHidden: StateFlow<Boolean?> = _isInfoHidden.asStateFlow()

    // Подписка на список стран в Room (маркеры на карте)
    val countryListFlow: Flow<List<CountryPresentation>> =
        homeRadioInteractor.subscribeOnCountryList().map { countries -> countries.map { it.toPresentation() } }

    init {
        viewModelScope.launch {
            _isInfoHidden.value = homeRadioInteractor.isHideInfoClicked()
        }
    }

    // Раньше состояние хранилось только в настройках, а LiveData помнила значение на момент открытия экрана:
    // после поворота экрана скрытый блок снова появлялся. Теперь состояние экрана и настройки меняются вместе
    fun setIsHideInfoClicked(isHideInfoClicked: Boolean) {
        _isInfoHidden.value = isHideInfoClicked
        viewModelScope.launch {
            homeRadioInteractor.setIsHideInfoClicked(isHideInfoClicked)
        }
    }
}