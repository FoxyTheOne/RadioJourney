package com.myproject.radiojourney.presentation.content.radioList

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.logOutUseCase.ILogOutUseCase
import com.myproject.radiojourney.domain.radioList.IRadioListUseCase
import com.myproject.radiojourney.utils.extension.call
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * Presentation layer, ViewModel. Работа с компонентами Android. Работает только с Interactor.
 *
 * Interactor - объект, который реализует UseCase, используя бизнес-объекты Entities.
 * Здесь осуществляется подписка, запрос через корутины.
 */
@HiltViewModel
class RadioListViewModel @Inject constructor(
    private val logOutInteractor: ILogOutUseCase,
    private val radioListInteractor: IRadioListUseCase
) : ViewModel() {
    companion object {
        private const val TAG = "RadioListViewModel"
    }

    // Получение списка радиостанций
    val radioStationListLiveData = MutableLiveData<List<RadioStationPresentation>>()

    // LiveData, которые будут отвечать за отображение прогресса (кружок)
    val showProgressLiveData = MutableLiveData<Boolean>()
    val hideProgressLiveData = MutableLiveData<Boolean>()

    // LiveData для открытия диалогового окна
    val dialogInternetTroubleLiveData = MutableLiveData<Boolean>()

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            showProgressLiveData.call()
            logOutInteractor.onLogout()
            hideProgressLiveData.call()
        }
    }

    fun getRadioStationList(countryCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val radioStationPresentation: List<RadioStationPresentation> =
                    radioListInteractor.getRadioStationList(countryCode)
                radioStationListLiveData.postValue(radioStationPresentation)
            } catch (e: IOException) {
                Log.d(TAG, "Exception: ${e.message}")
                dialogInternetTroubleLiveData.call()
                hideProgressLiveData.call()
            }
        }
    }
}