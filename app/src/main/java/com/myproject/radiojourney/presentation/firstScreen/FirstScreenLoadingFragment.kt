package com.myproject.radiojourney.presentation.firstScreen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.myproject.radiojourney.R
import com.myproject.radiojourney.databinding.LayoutFirstScreenLoadingBinding
import com.myproject.radiojourney.presentation.common.InfoDialog
import com.myproject.radiojourney.presentation.common.PermissionSessionState
import com.myproject.radiojourney.presentation.common.collectWhenStarted
import com.myproject.radiojourney.presentation.common.navigateSafely
import com.myproject.radiojourney.presentation.common.showPermissionDeniedDialog
import com.myproject.radiojourney.presentation.common.showPermissionRationale
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Приветственный экран: показывается только при первом запуске, пока скачивается список стран для карты.
 *
 * Что здесь происходит: полоса прогресса загрузки стран (WorkManager), кнопка "Start journey",
 * а после неё - объяснение и запрос разрешения на местоположение. Отказ не мешает войти: карта работает и без него.
 *
 * Если первый экран уже пройден, он больше не открывается: стартовый экран выбирает MainActivity
 * (раньше - BaseAuthFragmentAbstract, который переходил на карту прямо в onCreate этого фрагмента)
 */
@AndroidEntryPoint
class FirstScreenLoadingFragment : Fragment() {
    companion object {
        private const val TAG = "FirstScreenLoading"
        private const val EMPTY_LIST_DIALOG_DELAY = 7_000L
    }

    private var countryListIsNotEmpty = false
    private var emptyListDialogJob: Job? = null

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: LayoutFirstScreenLoadingBinding? = null
    private val viewModel by viewModels<FirstScreenLoadingViewModel>()
    private lateinit var infoDialog: InfoDialog

    // Какие разрешения уже запрашивали за этот запуск приложения (см. PermissionSessionState)
    @Inject
    lateinit var permissionSessionState: PermissionSessionState

    // Оформим запрос на PERMISSION, если он не был дан в предыдущий раз
    // !!! Т.к. запросов много, а не один, мы пишем .RequestMultiplePermissions() вместо .RequestPermission()
    // Т.обр., в лямбду к нам залетает не boolean, а map. ключом этого map будет string (наши permissions), а второе значение - это boolean
    // Следовательно, для обращения к определенному PERMISSION, мы обращаемся к нему по ключу типа permissionsMap[...] == true
    // Регистрируется полем класса: Activity Result API требует регистрации до создания фрагмента (раньше - в onViewCreated).
    // Разрешения на уведомления запрашивает MainActivity, а FOREGROUND_SERVICE выдаётся при установке и запроса не требует
    private val requestLocationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionsMap ->
            if (permissionsMap[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                ||
                permissionsMap[Manifest.permission.ACCESS_FINE_LOCATION] == true
            ) {
                // Если дано одно из разрешений, открываем следующий фрагмент
                openHomeRadio()
            } else {
                // Объясняем, что изменится без разрешения (раньше был Toast с текстом прямо в коде, без перевода),
                // и всё равно пускаем пользователя дальше: карта и радио работают и без местоположения.
                // Раньше отказ оставлял его на первом экране, и войти в приложение было нельзя
                requireContext().showPermissionDeniedDialog(
                    R.string.permission_location_title,
                    R.string.permission_location_denied_text,
                    isPermanentlyDenied = !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION),
                    onDismiss = { openHomeRadio() }
                )
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = LayoutFirstScreenLoadingBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Переход на контент в случае успешной аутентификации
        viewLifecycleOwner.collectWhenStarted(viewModel.signedIn) {
            // Если одно из разрешений уже есть, открываем HomeRadioFragment
            if (isLocationPermissionGranted()) {
                openHomeRadio()
            } else {
                // Сначала объясняем, зачем приложению местоположение, и только потом показываем системное окно.
                // Объяснение показываем один раз за запуск приложения (см. PermissionSessionState)
                val launchSystemRequest = {
                    requestLocationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    )
                }

                if (permissionSessionState.isFirstRequestInSession(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    requireContext().showPermissionRationale(
                        R.string.permission_location_title,
                        R.string.permission_location_text,
                        onContinue = launchSystemRequest
                    )
                } else {
                    launchSystemRequest()
                }
            }
        }

        infoDialog = InfoDialog(requireContext())

        binding?.buttonLogIn?.setOnClickListener {
            if (!countryListIsNotEmpty) {
                Log.d(TAG, "При нажатии на кнопку обнаружилось, что список кодов стран пустой")
                // Показываем диалоговое окно о проблеме с сервером
                infoDialog.show(R.string.dialogPleaseWait_title2, R.string.dialogPleaseWait_text2)
            } else {
                viewModel.onLoginClicked()
            }
        }

        // Подписываемся на локальную БД с помощью CountryListFlow (либо CountryListLiveData), аналогично подписке в HomeRadioFragment
        // Для того, чтобы знать, пустая ли база данных. По окончанию первого кеширования или же при последующих запусках покажем кнопку для входа, чтобы не ждать
        subscribeOnFlow()
    }

    private fun isLocationPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

    private fun openHomeRadio() {
        navigateSafely(R.id.action_firstScreenLoadingFragment_to_homeRadioFragment)
    }

    private fun subscribeOnFlow() {
        // Горизонтальная полоса прогресса загрузки списка стран (раньше - бродкаст из ProgressForegroundService)
        viewLifecycleOwner.collectWhenStarted(viewModel.countryCacheProgress) { progress ->
            binding?.progressBarHorizontal?.progress = progress
        }

        // viewLifecycleOwner, а не сам фрагмент: подписка живёт, пока существует экран (view), и не копится при возврате на фрагмент.
        // STARTED - когда экран не виден, данные не собираем (рекомендация developer.android.com для repeatOnLifecycle)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                viewModel.hasCountries.collect { hasCountries ->
                    if (hasCountries) {
                        emptyListDialogJob?.cancel()
                        infoDialog.hide()
                        Log.d(
                            TAG,
                            "При сборе данных в viewModel.hasCountries.collect список кодов стран НЕ пустой"
                        )
                        countryListIsNotEmpty = true

                        binding?.buttonLogIn?.isVisible = true
                        binding?.progressBarHorizontal?.isVisible = false
                    } else {
                        // Раньше - Handler.postDelayed: он срабатывал и после закрытия экрана, и dialog.show() у закрытой Activity
                        // ронял приложение (BadTokenException). Корутина viewLifecycleOwner отменяется вместе с экраном
                        emptyListDialogJob?.cancel()
                        emptyListDialogJob = viewLifecycleOwner.lifecycleScope.launch {
                            delay(EMPTY_LIST_DIALOG_DELAY)

                            // После задержки проверяем, может что-то поменялось
                            if (!countryListIsNotEmpty) {
                                Log.d(
                                    TAG,
                                    "При сборе данных в viewModel.hasCountries.collect список кодов стран всё ещё пустой, вызываем диалоговое окно"
                                )
                                // Показываем диалоговое окно о проблеме с сервером
                                infoDialog.show(
                                    R.string.dialogPleaseWait_title2,
                                    R.string.dialogPleaseWait_text2
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        // Открытый диалог нужно закрыть вместе с экраном, иначе WindowLeaked
        infoDialog.dismiss()
        super.onDestroyView()
        binding = null
    }
}