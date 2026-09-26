package com.myproject.radiojourney.presentation.content.homeRadio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.homeRadioUseCase.IHomeRadioUseCase
import com.myproject.radiojourney.presentation.model.CountryPresentation
import com.myproject.radiojourney.presentation.model.toPresentation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel главного экрана (карты): страны для маркеров и состояние информационного блока над картой.
 *
 * Страны приходят подпиской из базы (Flow), поэтому карта сама обновится, когда фоновая загрузка (CountryCacheWorker)
 * положит их в Room. Плеер внизу экрана к этой ViewModel не относится - им заведует MainViewModel
 */
@HiltViewModel
class HomeRadioViewModel @Inject constructor(
    private val homeRadioInteractor: IHomeRadioUseCase
) : ViewModel() {

    // Скрыт ли информационный блок. null - значение ещё не прочитано из настроек.
    // Значение приходит из DataStore: запись и чтение идут через одно и то же хранилище, поэтому после
    // пересоздания экрана (и даже после перезапуска приложения) блок остаётся в том же состоянии
    val isInfoHidden: StateFlow<Boolean?> = homeRadioInteractor.isHideInfoClicked()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Подписка на список стран в Room (маркеры на карте)
    val countryListFlow: Flow<List<CountryPresentation>> =
        homeRadioInteractor.subscribeOnCountryList()
            .map { countries -> countries.map { it.toPresentation() } }

    // Раньше состояние хранилось только в настройках, а LiveData помнила значение на момент открытия экрана:
    // после поворота экрана скрытый блок снова появлялся. Теперь состояние экрана и настройки меняются вместе
    fun setIsHideInfoClicked(isHideInfoClicked: Boolean) {
        viewModelScope.launch {
            homeRadioInteractor.setIsHideInfoClicked(isHideInfoClicked)
        }
    }
}