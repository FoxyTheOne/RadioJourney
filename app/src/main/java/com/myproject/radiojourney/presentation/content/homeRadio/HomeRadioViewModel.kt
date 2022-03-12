package com.myproject.radiojourney.presentation.content.homeRadio

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.homeRadio.IHomeRadioInteractor
import com.myproject.radiojourney.domain.logOut.ILogOutInteractor
import com.myproject.radiojourney.extension.call
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * ViewModel. Здесь осуществляется подписка, запрос через корутины. Работает с Interactor
 */
@HiltViewModel
class HomeRadioViewModel @Inject constructor(
    private val logOutInteractor: ILogOutInteractor,
    private val homeRadioInteractor: IHomeRadioInteractor
) : ViewModel() {
    companion object {
        private const val TAG = "HomeRadioViewModel"
    }

    // Подписка на локальную БД
    val countryListFlow = homeRadioInteractor.subscribeOnCountryList()

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

    fun getCountryListAndSaveToRoom() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                homeRadioInteractor.getCountryListAndSaveToRoom()
            } catch (e: IOException) {
                Log.d(TAG, "Exception: ${e.message}")
                dialogInternetTroubleLiveData.call()
                hideProgressLiveData.call()
            }
        }
    }

}