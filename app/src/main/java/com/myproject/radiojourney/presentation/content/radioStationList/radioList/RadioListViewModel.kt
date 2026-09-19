package com.myproject.radiojourney.presentation.content.radioStationList.radioList

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.radioListUseCase.IRadioListUseCase
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import com.myproject.radiojourney.other.Status
import com.myproject.radiojourney.utils.extension.call
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Presentation layer, ViewModel. Работа с компонентами Android. Работает только с Interactor.
 *
 * Interactor - объект, который реализует UseCase, используя бизнес-объекты Entities.
 * Здесь осуществляется подписка, запрос через корутины.
 */
@HiltViewModel
class RadioListViewModel @Inject constructor(
    private val radioListInteractor: IRadioListUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Аргументы навигации (код и название страны) ViewModel получает сама через SavedStateHandle
    val countryCode: String = savedStateHandle["country_code"] ?: ""
    val countryName: String = savedStateHandle["country_name"] ?: ""

    private val _radioStationListLiveData = MutableLiveData<List<RadioStationPresentation>>()
    val radioStationListLiveData: LiveData<List<RadioStationPresentation>> =
        _radioStationListLiveData

    private val _serverIsDownLiveData = MutableLiveData<Boolean>()
    val serverIsDownLiveData: LiveData<Boolean> = _serverIsDownLiveData

    init {
        // Список загружается один раз при создании ViewModel. Раньше загрузка запускалась из onViewCreated,
        // и при каждом возвращении на этот экран (кнопкой "назад") список заново скачивался с сервера
        loadRadioStationList()
    }

    // Получаем список радиостанций, преобразуем. Сохранять в Room не будем. Радиостанций очень много, будет занимать много места на телефоне.
    // Кроме того, списки на сервере постоянно обновляются. Возможно какой-то радиостанции в списке уже не будет, а в локальной БД она ещё осталась.
    private fun loadRadioStationList() {
        viewModelScope.launch(Dispatchers.IO) {
            val radioStationPresentationResource =
                radioListInteractor.getRadioStationList(countryCode)
            if (radioStationPresentationResource.status == Status.ERROR) {
                _serverIsDownLiveData.call()
            } else {
                _radioStationListLiveData.postValue(radioStationPresentationResource.data.orEmpty())
            }
        }
    }
}