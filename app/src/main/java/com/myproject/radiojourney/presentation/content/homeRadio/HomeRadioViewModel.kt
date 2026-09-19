package com.myproject.radiojourney.presentation.content.homeRadio

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.homeRadioUseCase.IHomeRadioUseCase
import com.myproject.radiojourney.entities.presentation.CountryPresentation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Presentation layer, ViewModel. Работа с компонентами Android. Работает только с Interactor.
 *
 * Interactor - объект, который реализует UseCase, используя бизнес-объекты Entities.
 * Здесь осуществляется подписка, запрос через корутины.
 */
@HiltViewModel
class HomeRadioViewModel @Inject constructor(
    private val homeRadioInteractor: IHomeRadioUseCase
) : ViewModel() {
    // Показать или скрыть информационный блок ("hide" / "show")
    private val _hideOrShowInfoLiveData = MutableLiveData<String>()
    val hideOrShowInfoLiveData: LiveData<String> = _hideOrShowInfoLiveData

    // Подписка на список стран в Room (маркеры на карте)
    val countryListFlow: Flow<List<CountryPresentation>> =
        homeRadioInteractor.subscribeOnCountryList()

    fun setIsHideInfoClicked(isHideInfoClicked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            homeRadioInteractor.setIsHideInfoClicked(isHideInfoClicked)
        }
    }

    fun hideOrShowInfoWhenFragmentCreated() {
        viewModelScope.launch(Dispatchers.IO) {
            val isHideInfoClickedInPreference = homeRadioInteractor.isHideInfoClicked()
            _hideOrShowInfoLiveData.postValue(if (isHideInfoClickedInPreference) "hide" else "show")
        }
    }
}