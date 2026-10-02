package com.myproject.radiojourney.presentation.firstScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.data.worker.CountryCacheScheduler
import com.myproject.radiojourney.domain.firstScreenLoadingUseCase.ILoginScreenUseCase
import com.myproject.radiojourney.domain.homeRadioUseCase.IHomeRadioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
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
    private val countryCacheScheduler: CountryCacheScheduler
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

    // Почему стран нет: null - всё в порядке (страны есть или загрузка идёт), иначе - что сказать пользователю.
    // combine пересчитывает ответ при каждом изменении базы или состояния загрузки; mapLatest отменяет начатую
    // паузу, если состояние успело смениться (загрузка стартовала - "нет интернета" так и не появится)
    @OptIn(ExperimentalCoroutinesApi::class)
    val countryListProblem: Flow<CountryListProblem?> =
        combine(hasCountries, countryCacheScheduler.status) { hasCountries, status ->
            if (hasCountries) null else status
        }.mapLatest { status ->
            when (status) {
                CountryCacheScheduler.Status.FAILED -> CountryListProblem.SERVER_UNAVAILABLE
                CountryCacheScheduler.Status.WAITING_FOR_NETWORK -> {
                    delay(NO_NETWORK_MESSAGE_DELAY)
                    CountryListProblem.NO_NETWORK
                }

                else -> null
            }
        }.distinctUntilChanged()

    enum class CountryListProblem { NO_NETWORK, SERVER_UNAVAILABLE }

    companion object {
        // Сколько ждём, прежде чем сказать "нет интернета": задача в очереди бывает и на долю секунды перед запуском
        private const val NO_NETWORK_MESSAGE_DELAY = 3_000L
    }

    // Кнопка "Повторить" в окне "сервер не отвечает": загрузка стран не удалась - запускаем её заново.
    // Раньше помогал только перезапуск приложения. Прошлая задача уже завершилась (FAILED), поэтому start()
    // ставит новую, и состояние на экране сменится на "идёт загрузка"
    fun retryCountryList() = countryCacheScheduler.start()

    // Раньше здесь были LiveData ошибки и диалога "нет интернета" в catch (AccountsException / IOException),
    // но сохранение токена такие исключения не бросает - эти ветки не могли сработать
    fun onLoginClicked() {
        viewModelScope.launch {
            loginScreenInteractor.onLoginClicked() // Сохраняем токен, чтобы в следующий раз пропустить этот фрагмент
            _signedIn.send(Unit)
        }
    }
}