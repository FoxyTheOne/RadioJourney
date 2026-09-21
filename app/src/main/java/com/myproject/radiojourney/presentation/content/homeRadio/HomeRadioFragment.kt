package com.myproject.radiojourney.presentation.content.homeRadio

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.CancellationTokenSource
import com.myproject.radiojourney.R
import com.myproject.radiojourney.databinding.LayoutHomeRadioBinding
import com.myproject.radiojourney.other.Constants.MAX_STATIONS_COUNT
import com.myproject.radiojourney.other.Constants.MY_STATIONS_COUNTRY_CODE
import com.myproject.radiojourney.presentation.MainViewModel
import com.myproject.radiojourney.presentation.common.InfoDialog
import com.myproject.radiojourney.presentation.common.PermissionSessionState
import com.myproject.radiojourney.presentation.common.collectWhenStarted
import com.myproject.radiojourney.presentation.common.showPermissionDeniedDialog
import com.myproject.radiojourney.presentation.common.showPermissionRationale
import com.myproject.radiojourney.presentation.content.base.BaseContentFragmentAbstract
import com.myproject.radiojourney.presentation.model.CountryPresentation
import com.myproject.radiojourney.presentation.model.RadioStationPresentation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Главная страница.
 * Содержит карту с метками, описание выбранной радиостанции и кнопки "добавить в избранное", "перейти в мой список".
 *
 * В этом фрагменте мы будем использовать Location API, а так же google maps
 * LOCATION -> 1.1. Прописать необходимые разрешения в манифесте
 * LOCATION -> 1.2. Имплементировать необходимую библиотеку (play services location)
 * LOCATION -> 1.3. Для доступа к местоположению, нужно разрешение. Логика запроса разрешения - в предыдущем фрагменте
 * LOCATION -> 1.4. Получим наш FusedLocationProviderClient. Именно он имеет в себе методы, с помощью которых мы можем определить локацию
 * GOOGLE MAPS -> 2. В инструкции от google всё делается в activity, а у нас - фрагмент. Следовательно, будут небольшие изменения
 */
@AndroidEntryPoint
class HomeRadioFragment : BaseContentFragmentAbstract(), OnMapReadyCallback {

    companion object {
        private const val TAG = "HomeRadioFragment"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: LayoutHomeRadioBinding? = null

    // 1.1. ViewModel. We bind our viewModel to the cycle of our activity, not fragment. So, we need to do this way:
    private lateinit var mainViewModel: MainViewModel

    private val viewModel by viewModels<HomeRadioViewModel>()

    private lateinit var infoDialog: InfoDialog

    // Переменная для нашего FusedLocationProviderClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // GOOGLE MAPS -> 2.1. Объявляем переменную, в соответствии с инструкцией от google
    private lateinit var mMap: GoogleMap

    // Карта текущего экрана готова. Фрагмент в back stack переживает свою view: mMap остаётся от старой, уже уничтоженной карты,
    // поэтому одной проверки ::mMap.isInitialized недостаточно
    private var isMapReady = false

    // GOOGLE MAPS -> 2.6. Объявляем переменную для маркера
    private var marker: Marker? = null
    private var customMarkerYouAreHere: Bitmap? = null
    private var customMarkerRadio: Bitmap? = null

    private var countryList = listOf<CountryPresentation>()

    private var locationCancellationTokenSource: CancellationTokenSource? = null

    // Какие разрешения уже запрашивали за этот запуск приложения (см. PermissionSessionState)
    @Inject
    lateinit var permissionSessionState: PermissionSessionState

    // Запрос разрешения на местоположение. Регистрируется полем класса: Activity Result API требует регистрации
    // до создания фрагмента. Раньше регистрация была в onViewCreated и повторялась при каждом возвращении на карту
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionsMap ->
            if (permissionsMap[Manifest.permission.ACCESS_COARSE_LOCATION] != true &&
                permissionsMap[Manifest.permission.ACCESS_FINE_LOCATION] != true
            ) {
                // Объясняем, что изменится без разрешения (раньше был Toast с текстом прямо в коде, без перевода)
                requireContext().showPermissionDeniedDialog(
                    R.string.permission_location_title,
                    R.string.permission_location_denied_text,
                    isPermanentlyDenied = !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            } else {
                getCurrentOrLastLocation(moveCamera = mainViewModel.mapCameraPosition == null)
            }
        }

    // Объясняем, зачем приложению местоположение, и только потом показываем системное окно запроса.
    // Объяснение показываем один раз за запуск приложения: при повторном запросе (кнопка "вы здесь")
    // пользователь уже знает, зачем это нужно, и сразу видит системное окно
    private fun requestLocationPermissionWithRationale() {
        val launchSystemRequest = {
            requestPermissionLauncher.launch(
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = LayoutHomeRadioBinding.inflate(inflater, container, false)
        // TOOLBAR
        // TOOLBAR - где будет находиться в нашем layout
        binding?.let { setToolbar(it.homeToolbar) }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbarMenu()

        // Если каким-то образом мы попали на этот фрагмент минуя первый, загрузочный фрагмент - стоит ещё раз проверить разрешения.
        // Сами запрашиваем только один раз за запуск приложения: карта создаётся заново при каждом возвращении на неё
        // и при повороте экрана, и запрос всплывал бы снова и снова. Позже его можно вызвать кнопкой "вы здесь"
        if (!isLocationPermissionGranted() &&
            !permissionSessionState.wasRequestedInSession(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            requestLocationPermissionWithRationale()
        }

        // 1.2. ViewModel. We bind our viewModel to the lifecycle of our activity, not fragment. We pass our activity as an owner of the lifecycle.
        // So, we need to do this way:
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        // Настройки диалогового окна
        infoDialog = InfoDialog(requireContext())
        infoDialog.showIfNoInternet()

        // LOCATION -> 1.4. Получим наш FusedLocationProviderClient. Именно он имеет в себе методы, с помощью которых мы можем определить локацию
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        if (arguments != null) {

            Log.d(
                TAG,
                "PLAYLIST_UPDATE: 1. Выбранная из списка станция передана в HomeRadioFragment"
            )
            var stationUuid = ""

            // 2. Получаем радиостанцию из списка на предыдущей странице, если перешли сюда из списка радиостанций. Определяем это по ключу "radio_station"
            // 2. Станция из списка "Мои радиостанции" (добавлена пользователем по ссылке)
            arguments?.parcelable<RadioStationPresentation>("my_station")
                ?.let { myStation ->
                    Log.d(TAG, "!! PLAYLIST_UPDATE: Передан аргумент с ключом my_station")
                    stationUuid = myStation.stationuuid

                    val curCountryCode =
                        mainViewModel.curPlayingSong.value?.mediaMetadata?.subtitle.toString()
                    if (curCountryCode == MY_STATIONS_COUNTRY_CODE) {
                        // Плейлист своих станций уже в плеере - просто включаем выбранную
                        mainViewModel.showConnectingProgress()
                    } else {
                        // В плеере другой плейлист: сначала загружаем свои станции (из базы, без сети)
                        mainViewModel.showDownloadingPlaylistProgress()
                        mainViewModel.fetchSongs(MY_STATIONS_COUNTRY_CODE)
                    }
                    mainViewModel.playOrToggleSong(myStation, false)
                    mainViewModel.notJustLaunchedEnableAutoplay()
                }

            arguments?.parcelable<RadioStationPresentation>("radio_station")
                ?.let { radioStation ->
                    Log.d(TAG, "!! PLAYLIST_UPDATE: Передан аргумент с ключом radio_station")
                    Log.d("UI_DEBUG", "PLAYER_DEBUG: Country selected: ${radioStation.countryCode}")
                    // Здесь мы получаем выбранную станцию из списка радиостанций по клику. Необходимо передать её в наш новый плейер
                    stationUuid = radioStation.stationuuid

                    val curCountryCode =
                        mainViewModel.curPlayingSong.value?.mediaMetadata?.subtitle.toString()
                    val argCountryCode = radioStation.countryCode

                    if (argCountryCode.endsWith("_FAV", true)) {
                        // Сюда мы попадаем, если из текущего плейлиста была выбрана favourite_station
                        handleArgumentsFavoriteStation(
                            radioStation,
                            curCountryCode = curCountryCode,
                            argCountryCode = argCountryCode
                        )
                    } else if (curCountryCode == argCountryCode && !curCountryCode.endsWith(
                            "_FAV",
                            true
                        )
                    ) {
                        // Одинаковый код страны и был включен НЕ FAV - Выбор из того же плейлиста
                        Log.d(
                            TAG,
                            "PLAYLIST_UPDATE: Выбранный элемент списка: $radioStation. Одинаковый код страны и был включен не FAV - Выбор из того же плейлиста, сountryCode = $argCountryCode"
                        )
                        mainViewModel.showConnectingProgress()

                        mainViewModel.playOrToggleSong(radioStation, false)
                        mainViewModel.notJustLaunchedEnableAutoplay()
                    } else if (curCountryCode == argCountryCode && curCountryCode.endsWith(
                            "_FAV",
                            true
                        )
                    ) {
                        // Одинаковый код страны, но был включен FAV - Значит загрузка нового плейлиста. Проблемный момент, если совпадает ещё и станция
                        Log.d(
                            TAG,
                            "PLAYLIST_UPDATE: Выбранный элемент списка: $radioStation. Одинаковый код страны, но был включен FAV - Значит загрузка нового плейлиста. Проблемный момент, если совпадает ещё и станция, сountryCode = $argCountryCode"
                        )
                        mainViewModel.showDownloadingPlaylistProgress()

                        mainViewModel.fetchSongs(radioStation.countryCode)
                        mainViewModel.playOrToggleSong(radioStation, false)
                        mainViewModel.notJustLaunchedEnableAutoplay()
                    } else {
                        // Загрузка нового плейлиста
                        Log.d(
                            TAG,
                            "PLAYLIST_UPDATE: Выбранный элемент списка: $radioStation. Другой код страны, загрузка нового плейлиста, сountryCode = $argCountryCode"
                        )
                        mainViewModel.showDownloadingPlaylistProgress()

                        mainViewModel.fetchSongs(radioStation.countryCode)
                        // Список стран передаёт ту станцию, с которой начнётся плейлист (самую популярную) - её и включаем
                        mainViewModel.playOrToggleSong(radioStation, false)
                        mainViewModel.notJustLaunchedEnableAutoplay()
                    }
                }

            // 2. Получаем радиостанцию из списка на предыдущей странице, если перешли сюда из списка избранных радиостанций
            arguments?.parcelable<RadioStationPresentation>("favourite_station")
                ?.let { radioStationFavourite ->
                    Log.d(TAG, "!! PLAYLIST_UPDATE: Передан аргумент с ключом favourite_station")
                    Log.d(
                        "UI_DEBUG",
                        "PLAYER_DEBUG: Country selected: ${radioStationFavourite.countryCode}"
                    )
                    // Здесь мы переходим из фрагмента "Избранное". Стоит загрузить в плейер плейлист избранного.
                    stationUuid = radioStationFavourite.stationuuid

                    val curCountryCode =
                        mainViewModel.curPlayingSong.value?.mediaMetadata?.subtitle.toString()
                    val argCountryCode = radioStationFavourite.countryCode

                    handleArgumentsFavoriteStation(
                        radioStationFavourite,
                        curCountryCode = curCountryCode,
                        argCountryCode = argCountryCode
                    )
                }

            // Автор API хочет, чтобы вы отправляли запрос /json/url каждый раз, когда пользователь кликает на радиостанцию. Это позволяет отмечать станции как популярные. Ваш запрос должен выглядеть примерно так:
            // String stationUrl = "https://de1.api.radio-browser.info/json/url/" + stationId;
            // где stationId - это идентификатор выбранной радиостанции.
            // !!! Когда пользователь кликает по радиостанции, он попадает сюда - открытие HomeFragment с аргументом, который прилетел из фрагмента с выбором радиостанций. Поэтому попробую строить логику начиная отсюда
            if (stationUuid.isNotEmpty() && stationUuid.isNotBlank()) {
                mainViewModel.markRadioStationAsPopularSendGetRequest(stationUuid)
            }

            // Аргумент обрабатываем один раз. Фрагмент остаётся в стеке навигации, и при возвращении на него кнопкой "Назад"
            // (например, из текущего плейлиста) onViewCreated вызывается снова с теми же аргументами - станция включалась
            // заново, даже если пользователь поставил плейер на паузу
            arguments?.remove("my_station")
            arguments?.remove("radio_station")
            arguments?.remove("favourite_station")

        } else if (mainViewModel.curPlayingSong.value == null) {
            if (!mainViewModel.isServerDown) {
                Log.d(
                    TAG,
                    "Аргументы равны нулю arguments = $arguments, curPlayingSong = null, сервер доступен isServerDown = ${mainViewModel.isServerDown} показываем полосу прогресса"
                )
                mainViewModel.showDownloadingPlaylistProgress()
            } else {
                infoDialog.show(R.string.dialogPleaseWait_title2, R.string.dialogPleaseWait_text5)
            }
        }

        // GOOGLE MAPS -> 2.2. Obtain the SupportMapFragment and get notified when the map is ready to be used.
        // Необходимо найти supportFragmentManager в списке всех фрагментов
        // val mapFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.map) не сработает,т.к. этот метод ищет внутри activity.
        // А наш supportFragmentManager находится не в activity: у нас есть HomeRadioFragment, а он - внутри этого HomeRadioFragment (т.е. фрагмент, внутри фрагмента)
        // Если мы открываем фрагмент внутри фрагмента, мы можем искать их с помощью childFragmentManager (заменяем, вместо supportFragmentManager)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        // Т.обр. мы говорим, что слушателем инициализации SupportMapFragment будет именно наш фрагмент (HomeRadioFragment)
        mapFragment.getMapAsync(this)
        // Testing Customize markers
        mapFragment.getMapAsync { googleMap ->
            // ADD MARKERS TO MAP -> 2. Здесь мы добавляем метки городов на карту
            // Set custom info window adapter
            googleMap.setInfoWindowAdapter(MarkerInfoWindowAdapter(requireContext()))
            googleMap.uiSettings.isZoomControlsEnabled =
                false // отключаем кнопки по умолчанию, чтобы настроить свои
        }

        // Иконки маркеров: векторная иконка, переведённая в Bitmap нужного размера.
        // Раньше было два одинаковых блока и ветки для SDK_INT < LOLLIPOP, которые никогда не выполнялись: minSdk приложения 23
        customMarkerYouAreHere =
            markerBitmap(R.drawable.ic_baseline_location_on_24_orange, R.drawable.marker, 100)
        customMarkerRadio =
            markerBitmap(R.drawable.ic_baseline_radio_24_orange, R.drawable.radio_icon4, 80)

        // LOCATION -> 1.5. Местоположение запрашивается в onMapReady (разово, при открытии фрагмента): маркер можно поставить только на готовую карту

        binding?.buttonYouAreHere?.setOnClickListener {
            if (isLocationPermissionGranted()) {
                getCurrentOrLastLocation()
            } else {
                // Если нет - объясняем, зачем нужно местоположение, и запрашиваем его
                requestLocationPermissionWithRationale()
            }
        }

        initListeners()

        // COUNTRY LIST MARKERS ON MAP -> 1. Получаем список кодов стран, преобразуем в локальные модели, сохраняем в Room. Будем делать эту работу в foreground service, чтобы отображать уведомление прогресса.
        // !!! Запустить нужно только 1 раз, при запуске программы, затем stopSelf() и больше этот сервис не запускать. Поэтому вызываем сервис из MainActivity

        // COUNTRY LIST MARKERS ON MAP -> 2. Затем подписываемся на локальную БД с помощью CountryListFlow (либо CountryListLiveData)
        subscribeOnFlow()
    }

    private fun initListeners() {
        binding?.apply {
            // Кнопки на карте
            buttonZoomPlus.setOnClickListener {
                mMap.animateCamera(CameraUpdateFactory.zoomIn())
            }
            buttonZoomMinus.setOnClickListener {
                mMap.animateCamera(CameraUpdateFactory.zoomOut())
            }

            // Закрыть информационный блок. Навсегда: текст можно перечитать в "О программе" (кнопка "i").
            // Раньше блок сворачивался стрелкой и разворачивался обратно
            buttonCloseInfo.setOnClickListener {
                viewModel.setIsHideInfoClicked(true)
            }
        }
        binding?.imageSettings?.setOnClickListener {
            if (this.findNavController().currentDestination?.id == R.id.homeRadioFragment) {
                this.findNavController()
                    .navigate(R.id.action_homeRadioFragment_to_settingsFragment)
            }
        }
        binding?.buttonGoToMyStations?.setOnClickListener {
            if (this.findNavController().currentDestination?.id == R.id.homeRadioFragment) {
                this.findNavController()
                    .navigate(R.id.action_homeRadioFragment_to_myStationsFragment)
            }
        }
        binding?.buttonGoToFavourites?.setOnClickListener {
            if (this.findNavController().currentDestination?.id == R.id.homeRadioFragment) {
                this.findNavController()
                    .navigate(R.id.action_homeRadioFragment_to_favouriteListFragment)
            }
        }
    }

    private fun subscribeOnFlow() {
        viewLifecycleOwner.collectWhenStarted(viewModel.isInfoHidden) { isHidden ->
            // Скрыть информационный блок. Состояние хранит HomeRadioViewModel (и записывает в настройки)
            if (isHidden != null) binding?.linearInfo?.isVisible =
                !isHidden // null - ещё не прочитано из настроек
        }

        // Полоса загрузки плейлиста или подключения к станции: блокируем кнопку избранного и показываем прогресс.
        // Раньше - три LiveData (setNonClickableDp, setNonClickableCRSt, setClickable), полностью повторяющие друг друга
        viewLifecycleOwner.collectWhenStarted(mainViewModel.loadingState) { loadingState ->
            val isLoading = loadingState != MainViewModel.LoadingState.NONE
            binding?.buttonGoToFavourites?.isClickable = !isLoading
            binding?.buttonGoToFavourites?.isEnabled = !isLoading
            if (isLoading) showProgress() else hideProgress()
            binding?.progressCircularLoadingArguments?.isVisible = isLoading
        }

        // viewLifecycleOwner, а не сам фрагмент. Фрагмент остаётся в back stack, когда открыт другой экран, а view уничтожается.
        // С lifecycleScope фрагмента подписка продолжала работать без экрана, а при каждом возвращении на карту добавлялась ещё одна
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                viewModel.countryListFlow.collect { countryPresentationList ->
                    binding?.textLoadingData?.isVisible = false
                    countryList =
                        countryPresentationList // Заполним массив для последующей обработки клика

                    // Раньше, если список приходил раньше карты, mMap ещё не был инициализирован: исключение молча ловилось,
                    // и маркеры стран не появлялись. Теперь маркеры рисуются здесь, если карта готова, или в onMapReady
                    showCountryMarkers()

                    if (countryPresentationList == emptyList<CountryPresentation>()) { // Мы переходим на эту страницу только если БД не пуста. Если массив пустой - что-то пошло не так
                        showProgress()
                        Toast.makeText(
                            context,
                            "Something went wrong. The server is down. Please, try again later",
                            Toast.LENGTH_LONG
                        ).show()

                        // Меняем текст диалогового окна
                        infoDialog.show(
                            R.string.dialogPleaseWait_title2,
                            R.string.dialogPleaseWait_text2
                        )
                    }
                }

            }
        }

    }

    private inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
        SDK_INT >= 33 -> getParcelable(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelable(key) as? T
    }

    // Векторная иконка в Bitmap заданного размера (маркеру карты нужен Bitmap). Если иконку получить не удалось - запасная картинка
    private fun markerBitmap(
        @DrawableRes vectorId: Int,
        @DrawableRes fallbackId: Int,
        sizePx: Int
    ): Bitmap {
        val drawable = AppCompatResources.getDrawable(requireContext(), vectorId)
            ?: requireNotNull(AppCompatResources.getDrawable(requireContext(), fallbackId))
        return drawable.toBitmap(sizePx, sizePx)
    }

    private fun showProgress() {
        binding?.frameLayout?.isVisible = true
        binding?.progressCircular?.isVisible = true
    }

    private fun hideProgress() {
        binding?.frameLayout?.isVisible = false
        binding?.progressCircular?.isVisible = false
    }

    // LOCATION -> 1.5. Создадим метод для получения Current location либо Last location
    // @SuppressLint("MissingPermission") - эта аннотация означает, что мы должны точно знать, что мы уже проверили, есть ли у нас PERMISSION
    // Т.к. на этот фрагмент мы попадаем только при согласии на получение местоположения, значит здесь разрешение у нас точно есть
    // Используем инициализированный fusedLocationProviderClient для доступа к методам
    @SuppressLint("MissingPermission")

    private fun getCurrentOrLastLocation(moveCamera: Boolean = true) {
        if (!isLocationPermissionGranted()) return // без разрешения запрос местоположения завершится ошибкой

        // Last location
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            location
                ?: return@addOnSuccessListener // Т.к. location может прилететь null, пишем тернарный оператор. Если location = null, то просто выходим из метода addOnSuccessListener
            Log.d(
                TAG,
                "LastLocation: latitude = ${location.latitude}, longitude = ${location.longitude}"
            ) // Проверяем получение ширины и долготы в логе

            // GOOGLE MAPS -> 2.4. Покажем на карте, где мы находимся (один раз). Создадим метод showMyLocation() и передадим туда текущее местоположение
            // <!-- 004 claude

            showMyLocation(LatLng(location.latitude, location.longitude), moveCamera)
        }

        // Current location
        // PRIORITY_PASSIVE - незначительное влияние на энергопотребление + получение обновлений местоположений, когда они доступны.
        // С этим параметром приложение не инициирует никаких обновлений местоположения, но получает местоположения, инициированные другими приложениями.
        // Раньше: устаревший LocationRequest.PRIORITY_NO_POWER и свой CancellationToken, который не сообщал об отмене слушателям.
        // CancellationTokenSource - стандартный способ отменить запрос (отменяем в onDestroyView, когда карты уже нет)
        locationCancellationTokenSource?.cancel()
        val cancellationTokenSource =
            CancellationTokenSource().also { locationCancellationTokenSource = it }
        fusedLocationProviderClient.getCurrentLocation(
            Priority.PRIORITY_PASSIVE,
            cancellationTokenSource.token
        ).addOnSuccessListener { location: Location? ->
            // Т.к. location может прилететь null, пишем тернарный оператор. Если location = null, то просто выходим из метода addOnSuccessListener
            location ?: return@addOnSuccessListener
            // Проверяем получение ширины и долготы в логе
            Log.d(
                TAG,
                "CurrentLocation: latitude = ${location.latitude}, longitude = ${location.longitude}"
            )

            // GOOGLE MAPS -> 2.4. Покажем на карте, где мы находимся (один раз). Создадим метод showMyLocation() и передадим туда текущее местоположение

            showMyLocation(LatLng(location.latitude, location.longitude), moveCamera)
        }
    }

    // GOOGLE MAPS -> 2.5. Покажем на карте, где мы находимся. Создадим метод showMyLocation() и передадим туда текущее местоположение
    // <!-- 004 claude

    private fun showMyLocation(latLng: LatLng, moveCamera: Boolean = true) {

        Log.d(
            TAG,
            "Метод showMyLocation вызван: latitude = ${latLng.latitude}, longitude = ${latLng.longitude}"
        )
        // Местоположение приходит асинхронно и может прийти, когда экрана уже нет
        if (!isMapReady || view == null) return

        // Настраиваем маркер, если он не null. Старый маркер убираем: lastLocation и getCurrentLocation приходят оба,
        // и раньше на карте появлялись два маркера "вы здесь"
        marker?.remove()
        customMarkerYouAreHere?.let { customBitmapMarker ->
            marker = mMap.addMarker(
                MarkerOptions()
                    .title(activity?.getString(R.string.homeRadio_youAreHere))
                    .snippet(null)
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.fromBitmap(customBitmapMarker))
            )
        }
        // И передвинем камеру
        // <!-- 004 claude

        if (moveCamera) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 5f))
    }

    // ADD MARKERS TO MAP -> 3. Здесь мы добавляем метки городов на карту
    // Adds marker representations of the places list on the provided GoogleMap object

    // Маркеры стран. Список хранится, чтобы при повторной выдаче списка из базы заменить маркеры, а не добавить копии поверх
    private val countryMarkers = mutableListOf<Marker>()

    private fun showCountryMarkers() {
        if (!isMapReady) return // карта ещё не готова - маркеры нарисует onMapReady
        showProgress()
        countryMarkers.forEach { it.remove() }
        countryMarkers.clear()
        countryList.forEach { countryPresentation ->
            addMarkersOnMap(countryPresentation)
        }
        Log.d(TAG, "На карте маркеров стран: ${countryMarkers.size}")
        hideProgress()
    }

    private fun addMarkersOnMap(countryPresentation: CountryPresentation) {
        customMarkerRadio?.let { customBitmapMarker ->
            val marker = mMap.addMarker(
                MarkerOptions()
                    .title(countryPresentation.countryName)
                    .snippet(
                        "Список радиостанций (${
                            minOf(
                                countryPresentation.stationCount,
                                MAX_STATIONS_COUNT
                            )
                        })"
                    ) // 004 claude // загружается не больше MAX_STATIONS_COUNT станций
                    .position(countryPresentation.countryLocation)
                    .icon(BitmapDescriptorFactory.fromBitmap(customBitmapMarker))
            )

            // Set place as the tag on the marker object so it can be referenced within
            // MarkerInfoWindowAdapter
            marker?.tag = countryPresentation
            marker?.let { countryMarkers += it }
        }
    }

    // GOOGLE MAPS -> 2.3. Сюда прилетит карта, когда она будет готова к работе.
    // Когда она сюда залетит, мы сможем к ней обратиться и сказать, что она будет равна нашей переменной класса
    // !!! This method passes a GoogleMap instance to you, which you can then use to perform various operations on the map.
    override fun onMapReady(map: GoogleMap) {
        this.mMap = map
        isMapReady = true

        // Фрагмент создаётся заново при каждом возвращении на главный экран. Чтобы карта не "загружалась заново",
        // возвращаем её туда, где пользователь её оставил (позиция хранится в MainViewModel, пока приложение открыто)
        mainViewModel.mapCameraPosition?.let { savedPosition ->
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(savedPosition))
        }
        mMap.setOnCameraIdleListener {
            mainViewModel.mapCameraPosition = mMap.cameraPosition
        }

        // Маркеры стран и "вы здесь" - только когда карта готова. Раньше местоположение запрашивалось в onViewCreated,
        // и если оно приходило раньше карты, mMap.addMarker ронял приложение (mMap ещё не инициализирован)
        showCountryMarkers()
        // Если пользователь уже двигал карту (позиция сохранена), не уводим её к маркеру "вы здесь" - к нему можно вернуться кнопкой
        getCurrentOrLastLocation(moveCamera = mainViewModel.mapCameraPosition == null)

        // Далее по документации здесь делают некоторые действия, однако мы сделаем их в отдельном методе

        // Обработка клика по InfoWindow маркера
        mMap.setOnInfoWindowClickListener { marker ->
            val latLon = marker.position

            // Страну берём из tag маркера. Раньше страна искалась по совпадению координат: у стран без координат (0, 0)
            // они совпадают, и открывалась первая из них. У маркера "вы здесь" tag нет - по нему ничего не открываем
            val country = marker.tag as? CountryPresentation
            if (country != null) {
                run {

                    Log.d(
                        TAG,
                        "Результат - выбран маркер: $latLon = ${country.countryLocation}, ${country.countryName}"
                    )

                    // Когда вернёмся на главный экран (посмотрев список, загрузив плейлист или просто назад), карта будет на этой стране
                    mainViewModel.mapCameraPosition =
                        CameraPosition.fromLatLngZoom(
                            country.countryLocation,
                            mMap.cameraPosition.zoom
                        )

                    // Перенесём countryCode на RadioListFragment для запроса списка станций
                    val direction =
                        HomeRadioFragmentDirections.actionHomeRadioFragmentToRadioListFragment(
                            country.countryCode,
                            country.countryName
                        )
                    if (this.findNavController().currentDestination?.id == R.id.homeRadioFragment) {
                        this.findNavController().navigate(direction)
                    }

                }
            }
        }
    }

    private fun handleArgumentsFavoriteStation(
        argRadioStationFavourite: RadioStationPresentation,
        curCountryCode: String,
        argCountryCode: String
    ) {

        if (curCountryCode == argCountryCode && !curCountryCode.endsWith(
                "_FAV",
                true
            )
        ) {
            // Одинаковый код страны, но был включен НЕ FAV - Выбор из другого плейлиста. Проблемный момент, если совпадает ещё и станция
            Log.d(
                TAG,
                "PLAYLIST_UPDATE: Выбранный элемент списка: $argRadioStationFavourite. Одинаковый код страны, но был включен НЕ FAV - Выбор из другого плейлиста. Проблемный момент, если совпадает ещё и станция, сountryCode = $argCountryCode"
            )
            mainViewModel.showDownloadingPlaylistProgress()

            mainViewModel.fetchSongs("FAV")
            mainViewModel.playOrToggleSong(argRadioStationFavourite, false)
            mainViewModel.notJustLaunchedEnableAutoplay()
        } else if (!curCountryCode.endsWith("_FAV", true)) {
            // Был включен НЕ FAV - Выбор из другого плейлиста
            Log.d(
                TAG,
                "PLAYLIST_UPDATE: Выбранный элемент списка: $argRadioStationFavourite. Был включен НЕ FAV - Выбор из другого плейлиста, сountryCode = $argCountryCode"
            )
            mainViewModel.showDownloadingPlaylistProgress()

            mainViewModel.fetchSongs("FAV")
            // Включаем именно переданную станцию: сессия дождётся плейлиста (onSetMediaItems).
            // Раньше станцию выбирала MainActivity сама, и порядок станций в плейлисте плеера не совпадал со списком избранного
            mainViewModel.playOrToggleSong(argRadioStationFavourite, false)
            mainViewModel.notJustLaunchedEnableAutoplay()
        } else {
            // Был включен FAV - Выбор из того же плейлиста
            Log.d(
                TAG,
                "PLAYLIST_UPDATE: Выбранный элемент списка: $argRadioStationFavourite. Выбор из того же плейлиста, сountryCode = $argCountryCode"
            )
            mainViewModel.showConnectingProgress()

            mainViewModel.playOrToggleSong(argRadioStationFavourite, false)
            mainViewModel.notJustLaunchedEnableAutoplay()
        }

    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        locationCancellationTokenSource?.cancel()
        locationCancellationTokenSource = null
        // Карта уничтожается вместе с view: маркеры старой карты больше не нужны
        isMapReady = false
        countryMarkers.clear()
        marker = null
        infoDialog.dismiss() // Открытый диалог закрываем вместе с экраном, иначе WindowLeaked
        super.onDestroyView()
        binding = null
    }

}