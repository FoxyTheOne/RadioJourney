package com.myproject.radiojourney.presentation.firstScreen

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.myproject.radiojourney.R
import com.myproject.radiojourney.databinding.LayoutFirstScreenLoadingBinding
import dagger.hilt.android.AndroidEntryPoint
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.myproject.radiojourney.entities.presentation.CountryPresentation
import com.myproject.radiojourney.other.Constants.FILTER_FOR_BROADCAST
import com.myproject.radiojourney.other.Constants.KEY_BROADCAST_COUNT
import com.myproject.radiojourney.other.Constants.KEY_BROADCAST_END
import com.myproject.radiojourney.other.Constants.KEY_BROADCAST_LIST_SIZE
import com.myproject.radiojourney.other.Status
import kotlinx.coroutines.launch

/**
 * Фрагмент для загрузки и входа в приложение.
 * Перед входом - запрос разрешения на определение местоположения.
 */
@AndroidEntryPoint
class FirstScreenLoadingFragment : BaseAuthFragmentAbstract() {

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
                    // Если дано одно из разрешений, открываем следующий фрагмент
                    this.findNavController()
                        .navigate(R.id.action_firstScreenLoadingFragment_to_homeRadioFragment)
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
                this.findNavController()
                    .navigate(R.id.action_firstScreenLoadingFragment_to_homeRadioFragment)
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

        initListeners()
        subscribeOnLiveData()

        // Подписываемся на локальную БД с помощью CountryListFlow (либо CountryListLiveData), аналогично подписке в HomeRadioFragment
        // Для того, чтобы знать, пустая ли база данных. По окончанию первого кеширования или же при последующих запусках покажем кнопку для входа, чтобы не ждать
        subscribeOnFlow()
    }

    // 3.Broadcast для горизонтальной полосы прогресса в фрагменте (1 - в сервисе)
    override fun onResume() {
        super.onResume()
        activity?.registerReceiver(receiver, IntentFilter(FILTER_FOR_BROADCAST))
    }

    // 3.Broadcast - регистрируем в onResume и отписываемся в onPause
    override fun onPause() {
        super.onPause()
        activity?.unregisterReceiver(receiver)
    }

    private fun initListeners() {
        binding?.buttonLogIn?.setOnClickListener {
            viewModel.onLoginClicked()
        }
    }

    private fun subscribeOnLiveData() {
        // Показываем или прячем Progress
        viewModel.showProgressLiveData.observe(viewLifecycleOwner) {
            showProgress()
        }
        viewModel.hideProgressLiveData.observe(viewLifecycleOwner) {
            hideProgress()
        }
        viewModel.dialogInternetTroubleLiveData.observe(viewLifecycleOwner) {
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
                    if (it != emptyList<CountryPresentation>()) {
                        binding?.buttonLogIn?.isVisible = true
                        binding?.progressBarHorizontal?.isVisible = false
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