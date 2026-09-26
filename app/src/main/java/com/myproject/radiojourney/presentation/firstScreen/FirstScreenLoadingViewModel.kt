package com.myproject.radiojourney.presentation.firstScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.data.worker.CountryCacheScheduler
import com.myproject.radiojourney.domain.firstScreenLoadingUseCase.ILoginScreenUseCase
import com.myproject.radiojourney.domain.homeRadioUseCase.IHomeRadioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel приветственного экрана: прогресс загрузки списка стран, есть ли уже страны в базе
 * и переход на карту по кнопке "Start journey".
 *
 * ViewModel хранит состояние экрана и переживает поворот телефона. Данные она берёт только у UseCase
 * (в этом проекте они называются Interactor), а Android-классов (View, Context) не касается
 */
@HiltViewModel
class FirstScreenLoadingViewModel @Inject constructor(
    private val loginScreenInteractor: ILoginScreenUseCase,
    homeRadioInteractor: IHomeRadioUseCase,
    countryCacheScheduler: CountryCacheScheduler
) : ViewModel() {
    // Прогресс загрузки списка стран (WorkManager) для полосы на экране. Раньше - бродкаст из ProgressForegroundService
    val countryCacheProgress: Flow<Int> = countryCacheScheduler.progress

    // Вход выполнен - экран проверит разрешения и перейдёт на карту. Channel: событие получит экран, даже если оно случилось,
    // пока экран не был виден, и получит один раз (раньше - LiveData<Boolean>, в которую "стреляли" значением true)
    private val _signedIn = Channel<Unit>(Channel.CONFLATED)
    val signedIn: Flow<Unit> = _signedIn.receiveAsFlow()

    // Подписка на локальную БД, для проверки (если БД пуста, нужно ждать окончания кеширования). Самих стран этому экрану не нужно
    val hasCountries: Flow<Boolean> =
        homeRadioInteractor.subscribeOnCountryList().map { it.isNotEmpty() }

    // Раньше здесь были LiveData ошибки и диалога "нет интернета" в catch (AccountsException / IOException),
    // но сохранение токена такие исключения не бросает - эти ветки не могли сработать
    fun onLoginClicked() {
        viewModelScope.launch {
            loginScreenInteractor.onLoginClicked() // Сохраняем токен, чтобы в следующий раз пропустить этот фрагмент
            _signedIn.send(Unit)
        }
    }
}