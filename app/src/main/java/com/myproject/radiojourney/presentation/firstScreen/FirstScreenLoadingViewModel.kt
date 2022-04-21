package com.myproject.radiojourney.presentation.firstScreen

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.homeRadio.IHomeRadioInteractor
import com.myproject.radiojourney.domain.signIn.ILoginScreenInteractor
import com.myproject.radiojourney.utils.extension.call
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FirstScreenLoadingViewModel @Inject constructor(
    private val loginScreenInteractor: ILoginScreenInteractor,
    private val homeRadioInteractor: IHomeRadioInteractor
) : ViewModel() {
    companion object {
        private const val TAG = "FirstScreenViewModel"
    }

    val signInLiveData = MutableLiveData<Boolean>()

    // Подписка на локальную БД
    val countryListFlow = homeRadioInteractor.subscribeOnCountryList()

    // LiveData, которые будут отвечать за отображение прогресса (кружок)
    val showProgressLiveData = MutableLiveData<Boolean>()
    val hideProgressLiveData = MutableLiveData<Boolean>()

    fun onLoginClicked() {
        viewModelScope.launch(Dispatchers.IO) {
            showProgressLiveData.call()
            loginScreenInteractor.onLoginClicked() // Сохраняем токен, чтобы в следующий раз пропустить этот фрагмент
            signInLiveData.call()
            hideProgressLiveData.call()
        }
    }
}