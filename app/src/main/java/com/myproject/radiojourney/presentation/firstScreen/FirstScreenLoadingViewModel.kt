package com.myproject.radiojourney.presentation.firstScreen

import android.accounts.AccountsException
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.homeRadioUseCase.IHomeRadioUseCase
import com.myproject.radiojourney.domain.firstScreenLoadingUseCase.ILoginScreenUseCase
import com.myproject.radiojourney.utils.extension.call
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
class FirstScreenLoadingViewModel @Inject constructor(
    private val loginScreenInteractor: ILoginScreenUseCase,
    homeRadioInteractor: IHomeRadioUseCase
) : ViewModel() {
    val failedLiveData = MutableLiveData<Boolean>()

    // LiveData для открытия диалогового окна
    val dialogInternetTroubleLiveData = MutableLiveData<Boolean>()

    // Флаг для проверки на permissions при переходе на следующий fragment
    val signInLiveData = MutableLiveData<Boolean>()

    // Подписка на локальную БД, для проверки (Если БД пуста, нужно ждать окончания кеширования)
    val countryListFlow = homeRadioInteractor.subscribeOnCountryList()

    // LiveData, которые будут отвечать за отображение прогресса (кружок)
    val showProgressLiveData = MutableLiveData<Boolean>()
    val hideProgressLiveData = MutableLiveData<Boolean>()

    fun onLoginClicked() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                showProgressLiveData.call()
                loginScreenInteractor.onLoginClicked() // Сохраняем токен, чтобы в следующий раз пропустить этот фрагмент
                signInLiveData.call()
                hideProgressLiveData.call()
            } catch (e1: AccountsException) {
                e1.printStackTrace()
                dialogInternetTroubleLiveData.call()
            } catch (e: IOException) {
                e.printStackTrace()
                failedLiveData.call()
            }
        }
    }
}