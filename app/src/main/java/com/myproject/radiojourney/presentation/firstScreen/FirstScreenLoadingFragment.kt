package com.myproject.radiojourney.presentation.firstScreen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.myproject.radiojourney.R
import com.myproject.radiojourney.databinding.LayoutFirstScreenLoadingBinding
import com.myproject.radiojourney.entities.presentation.CountryPresentation
import com.myproject.radiojourney.presentation.common.InfoDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Фрагмент для загрузки и входа в приложение.
 * Перед входом - запрос разрешения на определение местоположения.
 *
 * Если пользователь уже входил, этот экран не открывается: стартовый экран выбирает MainActivity
 * (раньше - BaseAuthFragmentAbstract, который переходил на карту прямо в onCreate этого фрагмента)
 */
@AndroidEntryPoint
class FirstScreenLoadingFragment : Fragment() {
    companion object {
        private const val TAG = "FirstScreenLoading"
        private const val EMPTY_LIST_DIALOG_DELAY = 7_000L
    }

    private var countryList = emptyList<CountryPresentation>()
    private var countryListIsNotEmpty = false
    private var emptyListDialogJob: Job? = null

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: LayoutFirstScreenLoadingBinding? = null
    private val viewModel by viewModels<FirstScreenLoadingViewModel>()
    private lateinit var infoDialog: InfoDialog

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
                Toast.makeText(
                    requireContext(),
                    "We can't show your location without an access to it",
                    Toast.LENGTH_LONG
                ).show()
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
        viewModel.signInLiveData.observe(viewLifecycleOwner) {
            // Если одно из разрешений уже есть, открываем HomeRadioFragment
            if (isLocationPermissionGranted()) {
                openHomeRadio()
            } else {
                // Если нет - вызываем requestPermissionLauncher
                requestLocationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            }
        }

        // Настройки диалогового окна
        infoDialog = InfoDialog(requireContext())

        binding?.buttonLogIn?.setOnClickListener {

            if (countryList.isEmpty()) {
                Log.d(TAG, "При нажатии на кнопку обнаружилось, что список кодов стран пустой")
                // Показываем диалоговое окно о проблеме с сервером
                infoDialog.show(R.string.dialogPleaseWait_title2, R.string.dialogPleaseWait_text2)
            } else {
                viewModel.onLoginClicked()
            }
        }

//        initListeners()
        subscribeOnLiveData()

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
        if (this.findNavController().currentDestination?.id == R.id.firstScreenLoadingFragment) {
            this.findNavController()
                .navigate(R.id.action_firstScreenLoadingFragment_to_homeRadioFragment)
        }
    }

    private fun subscribeOnLiveData() {
        // Горизонтальная полоса прогресса загрузки списка стран (раньше - бродкаст из ProgressForegroundService)
        viewModel.countryCacheProgressLiveData.observe(viewLifecycleOwner) { progress ->
            binding?.progressBarHorizontal?.progress = progress
        }
    }

    private fun subscribeOnFlow() {
        // viewLifecycleOwner, а не сам фрагмент: подписка живёт, пока существует экран (view), и не копится при возврате на фрагмент.
        // STARTED - когда экран не виден, данные не собираем (рекомендация developer.android.com для repeatOnLifecycle)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                viewModel.countryListFlow.collect {
                    if (it.isNotEmpty()) {
                        emptyListDialogJob?.cancel()
                        infoDialog.hide()
                        Log.d(
                            TAG,
                            "При сборе данных в viewModel.countryListFlow.collect список кодов стран НЕ пустой"
                        )

                        countryList = it
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
                                    "При сборе данных в viewModel.countryListFlow.collect список кодов стран всё ещё пустой, вызываем диалоговое окно"
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