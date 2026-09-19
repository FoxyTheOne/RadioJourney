package com.myproject.radiojourney.presentation.firstScreen

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.data.worker.CountryCacheScheduler
import com.myproject.radiojourney.domain.firstScreenLoadingUseCase.ILoginScreenUseCase
import com.myproject.radiojourney.domain.homeRadioUseCase.IHomeRadioUseCase
import com.myproject.radiojourney.utils.extension.call
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
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
    homeRadioInteractor: IHomeRadioUseCase,
    countryCacheScheduler: CountryCacheScheduler
) : ViewModel() {
    // Прогресс загрузки списка стран (WorkManager) для полосы на экране. Раньше - бродкаст из ProgressForegroundService
    val countryCacheProgressLiveData: LiveData<Int> = countryCacheScheduler.progressLiveData

    // Флаг для проверки на permissions при переходе на следующий fragment
    private val _signInLiveData = MutableLiveData<Boolean>()
    val signInLiveData: LiveData<Boolean> = _signInLiveData

    // Подписка на локальную БД, для проверки (Если БД пуста, нужно ждать окончания кеширования)
    val countryListFlow = homeRadioInteractor.subscribeOnCountryList()

    // Раньше здесь были LiveData ошибки и диалога "нет интернета" в catch (AccountsException / IOException),
    // но сохранение токена такие исключения не бросает - эти ветки не могли сработать
    fun onLoginClicked() {
        viewModelScope.launch {
            loginScreenInteractor.onLoginClicked() // Сохраняем токен, чтобы в следующий раз пропустить этот фрагмент
            _signInLiveData.call()
        }
    }
}