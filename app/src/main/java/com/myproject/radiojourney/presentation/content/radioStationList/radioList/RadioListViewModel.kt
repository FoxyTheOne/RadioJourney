package com.myproject.radiojourney.presentation.content.radioStationList.radioList

import android.accounts.AccountsException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.radioListUseCase.IRadioListUseCase
import com.myproject.radiojourney.utils.extension.call
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import com.myproject.radiojourney.other.Event
import com.myproject.radiojourney.other.Resource
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
    private val radioListInteractor: IRadioListUseCase
) : ViewModel() {

//    companion object {
//        private const val TAG = "RadioListViewModel"
//    }

    // Получение списка радиостанций
    private val _radioStationListLiveData = MutableLiveData<List<RadioStationPresentation>>()
    val radioStationListLiveData: MutableLiveData<List<RadioStationPresentation>> =
        _radioStationListLiveData

    // LiveData, которые будут отвечать за отображение прогресса (кружок)
    private val _showProgressLiveData = MutableLiveData<Boolean>()
    val showProgressLiveData: MutableLiveData<Boolean> = _showProgressLiveData
    private val _hideProgressLiveData = MutableLiveData<Boolean>()
    val hideProgressLiveData: MutableLiveData<Boolean> = _hideProgressLiveData

    // If smth went wrong
    private val _errorMessageLiveData =
        MutableLiveData<Event<Resource<Boolean>>>() // It must be private, so that other classes can't change it
    val errorMessageLiveData: LiveData<Event<Resource<Boolean>>> =
        _errorMessageLiveData // And another LiveData, that equals to previous, so that classes can't change it

    // LiveData для открытия диалогового окна
    private val _dialogInternetTroubleLiveData = MutableLiveData<Boolean>()
    val dialogInternetTroubleLiveData: MutableLiveData<Boolean> = _dialogInternetTroubleLiveData

    fun getRadioStationList(countryCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val radioStationPresentation: List<RadioStationPresentation> =
                    radioListInteractor.getRadioStationList(countryCode)
                _radioStationListLiveData.postValue(radioStationPresentation)
            } catch (e1: AccountsException) {
                // AccountsException -> Known direct subclasses: AuthenticatorException, NetworkErrorException, OperationCanceledException
                e1.printStackTrace()
                _dialogInternetTroubleLiveData.call()
                _hideProgressLiveData.call()
            } catch (e: IOException) {
                e.printStackTrace()
                // TODO всплывающее окно об ошибке скачивания плейлиста. Проверьте интернет-соединение и повторите действие ещё раз
                _errorMessageLiveData.postValue(
                    Event(
                        Resource.error(
                            "Failed connecting to the local database",
                            null
                        )
                    )
                )
                _hideProgressLiveData.call()
            }
        }
    }

}