package com.myproject.radiojourney.presentation.content.homeRadio

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.logOut.ILogOutInteractor
import com.myproject.radiojourney.extension.call
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeRadioViewModel @Inject constructor(
    private val logOutInteractor: ILogOutInteractor
) : ViewModel() {
    // LiveData, которые будут отвечать за отображение прогресса (кружок)
    val showProgressLiveData = MutableLiveData<Boolean>()
    val hideProgressLiveData = MutableLiveData<Boolean>()

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            showProgressLiveData.call()
            logOutInteractor.onLogout()
            hideProgressLiveData.call()
        }
    }
}