package com.myproject.radiojourney.presentation.firstScreen

import android.Manifest
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.myproject.radiojourney.R
import com.myproject.radiojourney.databinding.LayoutFirstScreenLoadingBinding
import com.myproject.radiojourney.entities.presentation.CountryPresentation
import com.myproject.radiojourney.other.Constants.FILTER_FOR_BROADCAST
import com.myproject.radiojourney.other.Constants.KEY_BROADCAST_COUNT
import com.myproject.radiojourney.other.Constants.KEY_BROADCAST_END
import com.myproject.radiojourney.other.Constants.KEY_BROADCAST_LIST_SIZE
import com.myproject.radiojourney.other.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Фрагмент для загрузки и входа в приложение.
 * Перед входом - запрос разрешения на определение местоположения.
 */
@AndroidEntryPoint
class FirstScreenLoadingFragment : BaseAuthFragmentAbstract() {
    companion object {
        private const val TAG = "FirstScreenLoading"
    }

    private var countryList = emptyList<CountryPresentation>()
    private var countryListIsNotEmpty = false

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: LayoutFirstScreenLoadingBinding? = null
    private val viewModel by viewModels<FirstScreenLoadingViewModel>()
    private lateinit var dialogInternetTrouble: Dialog

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

        // Запрос на разрешение Foreground
        val requestPermissionLauncherForeground =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (!isGranted) {
                    Toast.makeText(
                        requireContext(),
                        "We don't have permission to start foreground service",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        // Запрос на разрешение notification
        val requestPermissionLauncherNotification =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (!isGranted) {
                    Toast.makeText(
                        requireContext(),
                        "We don't have permission to show notifications on your Android",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        // Оформим запрос на PERMISSION, если он не был дан в предыдущий раз
        // !!! Т.к. запросов много, а не один, мы пишем .RequestMultiplePermissions() вместо .RequestPermission()
        // Т.обр., в лямбду к нам залетает не boolean, а map. ключом этого map будет string (наши permissions), а второе значение - это boolean
        // Следовательно, для обращения к определенному PERMISSION, мы обращаемся к нему по ключу типа permissionsMap[...] == true
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissionsMap ->
                if (permissionsMap[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    ||
                    permissionsMap[Manifest.permission.ACCESS_FINE_LOCATION] == true
                ) {

                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.FOREGROUND_SERVICE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // Если нет разрешения - вызываем requestPermissionLauncher
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            requestPermissionLauncherForeground.launch(Manifest.permission.FOREGROUND_SERVICE)
                        }
                    }

                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // Если нет разрешения - вызываем requestPermissionLauncher
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestPermissionLauncherNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    // Если дано одно из разрешений, открываем следующий фрагмент
//                    this.findNavController()
//                        .navigate(R.id.action_firstScreenLoadingFragment_to_homeRadioFragment)
                    if (this.findNavController().currentDestination?.id == R.id.firstScreenLoadingFragment) {
                        this.findNavController()
                            .navigate(R.id.action_firstScreenLoadingFragment_to_homeRadioFragment)
                    }

                } else {
                    Toast.makeText(
                        requireContext(),
                        "We can't show your location without an access to it",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        // Переход на контент в случае успешной аутентификации
        viewModel.signInLiveData.observe(viewLifecycleOwner) {
            // Если одно из разрешений уже есть, открываем LocationFragment
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {

                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.FOREGROUND_SERVICE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Если нет разрешения - вызываем requestPermissionLauncher
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        requestPermissionLauncherForeground.launch(Manifest.permission.FOREGROUND_SERVICE)
                    }
                }

                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Если нет разрешения - вызываем requestPermissionLauncher
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionLauncherNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

//                this.findNavController()
//                    .navigate(R.id.action_firstScreenLoadingFragment_to_homeRadioFragment)
                if (this.findNavController().currentDestination?.id == R.id.firstScreenLoadingFragment) {
                    this.findNavController()
                        .navigate(R.id.action_firstScreenLoadingFragment_to_homeRadioFragment)
                }

            } else {
                // Если нет - вызываем requestPermissionLauncher
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            }
        }

        // Настройки диалогового окна
        dialogInternetTrouble = Dialog(requireContext())
        // Передайте ссылку на разметку
        dialogInternetTrouble.setContentView(R.layout.layout_internet_trouble_dialog)

        binding?.buttonLogIn?.setOnClickListener {

            if (countryList.isEmpty()) {
                Log.d(TAG, "При нажатии на кнопку обнаружилось, что список кодов стран пустой")
                // Показываем диалоговое окно о проблеме с сервером
                // Меняем текст диалогового окна
                val dialogSmthWentWrongTitle = getString(R.string.dialogPleaseWait_title2)
                val dialogSmthWentWrongText = getString(R.string.dialogPleaseWait_text2)
                val dialogSmthWentWrongTitleView =
                    dialogInternetTrouble.findViewById<AppCompatTextView>(R.id.title_internetTrouble)
                val dialogSmthWentWrongTextView =
                    dialogInternetTrouble.findViewById<AppCompatTextView>(R.id.text_internetTrouble)
                dialogSmthWentWrongTitleView.text = dialogSmthWentWrongTitle
                dialogSmthWentWrongTextView.text = dialogSmthWentWrongText

                dialogInternetTrouble.show()

            } else {

                viewModel.onLoginClicked()

                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.FOREGROUND_SERVICE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Если нет разрешения - вызываем requestPermissionLauncher
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        requestPermissionLauncherForeground.launch(Manifest.permission.FOREGROUND_SERVICE)
                    }
                }

                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Если нет разрешения - вызываем requestPermissionLauncher
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionLauncherNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

            }

        }

//        initListeners()
        subscribeOnLiveData()

        // Подписываемся на локальную БД с помощью CountryListFlow (либо CountryListLiveData), аналогично подписке в HomeRadioFragment
        // Для того, чтобы знать, пустая ли база данных. По окончанию первого кеширования или же при последующих запусках покажем кнопку для входа, чтобы не ждать
        subscribeOnFlow()
    }

    // 3.Broadcast для горизонтальной полосы прогресса в фрагменте (1 - в сервисе)
    override fun onResume() {
        super.onResume()
        activity?.let {
            ContextCompat.registerReceiver(
                it,
                receiver,
                IntentFilter(FILTER_FOR_BROADCAST),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
//        activity?.registerReceiver(receiver, IntentFilter(FILTER_FOR_BROADCAST))
    }

    // 3.Broadcast - регистрируем в onResume и отписываемся в onPause
    override fun onPause() {
        super.onPause()
        activity?.unregisterReceiver(receiver)
    }

//    private fun initListeners() {
//        binding?.buttonLogIn?.setOnClickListener {
//            viewModel.onLoginClicked()
//        }
//    }

    private fun subscribeOnLiveData() {
        // Показываем или прячем Progress
        viewModel.showProgressLiveData.observe(viewLifecycleOwner) {
            showProgress()
        }
        viewModel.hideProgressLiveData.observe(viewLifecycleOwner) {
            hideProgress()
        }
        viewModel.dialogInternetTroubleLiveData.observe(viewLifecycleOwner) {
            // Уточняем текст диалогового окна (который по умолчанию)
            val dialogInternetTroubleTitle = getString(R.string.dialogInternetTrouble_title)
            val dialogInternetTroubleText = getString(R.string.dialogInternetTrouble_text)
            val dialogInternetTroubleTitleView =
                dialogInternetTrouble.findViewById<AppCompatTextView>(R.id.title_internetTrouble)
            val dialogInternetTroubleTextView =
                dialogInternetTrouble.findViewById<AppCompatTextView>(R.id.text_internetTrouble)
            dialogInternetTroubleTitleView.text = dialogInternetTroubleTitle
            dialogInternetTroubleTextView.text = dialogInternetTroubleText

            dialogInternetTrouble.show()
        }
        viewModel.errorMessageLiveData.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandled()?.let { result ->
                when (result.status) {
                    // If everything is ok, we don't want to show anything. Only if smth went wrong
                    Status.ERROR ->
                        binding?.let { nonNullBinding ->
                            Snackbar.make(
                                nonNullBinding.frameLayout.rootView,
                                result.message ?: "An unknown error occurred",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }

                    else -> Unit
                }
            }
        }
    }

    private fun subscribeOnFlow() {

        // Function launchWhenCreated is deprecated as it can lead to wasted resources in some cases.
        // Replace with suspending repeatOnLifecycle to run the block whenever the Lifecycle state is at least Lifecycle.State.CREATED.

//        lifecycleScope.launchWhenCreated {
//            viewModel.countryListFlow.collect {
//                if (it != listOf<CountryPresentation>()) {
//                    binding?.buttonLogIn?.isVisible = true
//                    binding?.progressBarHorizontal?.isVisible = false
//                }
//            }
//        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {

                viewModel.countryListFlow.collect {
//                    if (it != listOf<CountryPresentation>()) {
//                    if (it != emptyList<CountryPresentation>()) {
                    if (it.isNotEmpty()) {
                        dialogInternetTrouble.hide()
                        Log.d(
                            TAG,
                            "При сборе данных в viewModel.countryListFlow.collect список кодов стран НЕ пустой"
                        )

                        countryList = it
                        countryListIsNotEmpty = true

                        binding?.buttonLogIn?.isVisible = true
                        binding?.progressBarHorizontal?.isVisible = false
                    } else {
                        android.os.Handler(Looper.getMainLooper()).postDelayed({

                            Log.d(
                                TAG,
                                "При сборе данных в viewModel.countryListFlow.collect обнаружилось, что список кодов стран пустой"
                            )
                            // После задержки проверяем, может что-то поменялось
                            if (countryListIsNotEmpty) {
                                Log.d(
                                    TAG,
                                    "При сборе данных в viewModel.countryListFlow.collect в следующий раз список кодов стран заполнился, диалоговое окно не вызываем"
                                )
                            } else {
                                Log.d(
                                    TAG,
                                    "При сборе данных в viewModel.countryListFlow.collect список кодов стран всё ещё пустой, вызываем диалоговое окно"
                                )
                                // Показываем диалоговое окно о проблеме с сервером
                                // Меняем текст диалогового окна
                                val dialogSmthWentWrongTitle =
                                    getString(R.string.dialogPleaseWait_title2)
                                val dialogSmthWentWrongText =
                                    getString(R.string.dialogPleaseWait_text2)
                                val dialogSmthWentWrongTitleView =
                                    dialogInternetTrouble.findViewById<AppCompatTextView>(R.id.title_internetTrouble)
                                val dialogSmthWentWrongTextView =
                                    dialogInternetTrouble.findViewById<AppCompatTextView>(R.id.text_internetTrouble)
                                dialogSmthWentWrongTitleView.text = dialogSmthWentWrongTitle
                                dialogSmthWentWrongTextView.text = dialogSmthWentWrongText

                                dialogInternetTrouble.show()
                            }

                        }, 7000)
                    }

                }

            }
        }

    }

    private fun showProgress() {
        binding?.frameLayout?.isVisible = true
        binding?.progressCircular?.isVisible = true
    }

    private fun hideProgress() {
        binding?.frameLayout?.isVisible = false
        binding?.progressCircular?.isVisible = false
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    // 2.Broadcast для горизонтальной полосы прогресса в фрагменте (1 - в сервисе)
    // Создадим анонимный класс => не нужно регистрировать в манифесте
    private var receiver: BroadcastReceiver? = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val listSize = intent.getIntExtra(KEY_BROADCAST_LIST_SIZE, 1)
            val filesAmount = intent.getIntExtra(KEY_BROADCAST_COUNT, 1)
            val endOfBroadcast = intent.getIntExtra(KEY_BROADCAST_END, 1)

//            var progress = binding?.progressBarHorizontal?.progress
//            progress = progress?.plus(10)
//            progress?.let { binding?.progressBarHorizontal?.setProgress(it) }

            val progress = 100 * filesAmount / listSize
            binding?.progressBarHorizontal?.progress = progress

            if (endOfBroadcast == 100) {
                binding?.progressBarHorizontal?.progress = 100
            }
        }
    }
}