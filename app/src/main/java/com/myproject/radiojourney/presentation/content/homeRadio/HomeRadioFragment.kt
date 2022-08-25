package com.myproject.radiojourney.presentation.content.homeRadio

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.*
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.myproject.radiojourney.IAppSettings
import com.myproject.radiojourney.R
import com.myproject.radiojourney.presentation.content.base.BaseContentFragmentAbstract
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import javax.inject.Inject
import com.myproject.radiojourney.entities.presentation.CountryPresentation
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import android.widget.Toast
import com.myproject.radiojourney.databinding.LayoutHomeRadioBinding
import com.myproject.radiojourney.utils.musicPlayer.*
import kotlinx.coroutines.*
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import com.myproject.radiojourney.other.Constants
import com.myproject.radiojourney.other.Constants.MUSIC_PLAYER_SERVICE_FAILURE_PLAYING_BROADCAST
import com.myproject.radiojourney.other.Constants.NOTIFICATION_MUSIC_ACTION_BROADCAST
import com.myproject.radiojourney.other.Status
import com.myproject.radiojourney.presentation.MainViewModel

/**
 * Главная страница.
 * Содержит карту с метками, описание выбранной радиостанции и кнопки "добавить в избранное", "перейти в мой список".
 *
 * В этом фрагменте мы будем использовать Location API, а так же google maps
 * LOCATION -> 1.1. Прописать необходимые разрешения в манифесте
 * LOCATION -> 1.2. Имплементировать необходимую библиотеку (play services location)
 * LOCATION -> 1.3. Для доступа к местоположению, нужно разрешение. Логика запроса разрешения - в предыдущем фрагменте
 * LOCATION -> 1.4. Получим наш FusedLocationProviderClient. Именно он имеет в себе методы, с помощью которых мы можем определить локацию
 * GOOGLE MAPS -> 2. В инструкции от гугла всё делается в activity, а у нас - фрагмент. Следовательно, будут небольшие изменения
 *
 * TODO повторить проверку разрешения определения местоположения
 */
@AndroidEntryPoint
class HomeRadioFragment : BaseContentFragmentAbstract(), OnMapReadyCallback {
    // MUSIC PLAYER ON NOTIFICATION -> 7. Implements IPlayable
    companion object {
        private const val TAG = "HomeRadioFragment"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: LayoutHomeRadioBinding? = null

    @Inject
    lateinit var appSettings: IAppSettings

    private val viewModel by viewModels<HomeRadioViewModel>()


    // 1.1. ViewModel. We bind our viewModel to the cycle of our activity, not fragment. So, we need to do this way:
    lateinit var mainViewModel: MainViewModel


    private lateinit var dialogInternetTrouble: Dialog

    //    private lateinit var notificationManager: NotificationManager
//    private var isPaused = true
    private var isStationSelected = false

    // Переменная для нашего FusedLocationProviderClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // GOOGLE MAPS -> 2.1. Объявляем переменную, в соответствии с инструкцией от гугла
    private lateinit var mMap: GoogleMap

    // GOOGLE MAPS -> 2.6. Объявляем переменную для маркера
    private var marker: Marker? = null
    private var customMarkerYouAreHere: Bitmap? = null
    private var customMarkerRadio: Bitmap? = null

    //    // ADD MARKERS TO MAP -> 1. Для примера, сейчас. Потом подгружать список по запросу
    //    private val places: List<Place> = listOf(
    //        Place(name = "Minsk", latLng = LatLng(53.90580039557321, 27.562806971874416))
    //    )
    private var countryList = listOf<CountryPresentation>()

//    // BOUND_SERVICE -> 8. Создадим наш Service connection (второй параметр при запуске сервиса с помощью Intent)
//    // BOUND_SERVICE -> 8.1. Создадим переменную, чтобы инициализировать её при создании Service connection
//    private var iMusicPlayerBinder: IMusicPlayerBinder? = null

//    // BOUND_SERVICE -> 8.2. Создадим экземпляр Service connection
//    private val connection = object : ServiceConnection {
//        // Когда мы забандимся к нашему сервису, вызовется метод onServiceConnected() и мы получим экземпляр binder: IBinder?
//        override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
//            // Проверяем binder на null. Если он не null, приводим к типу нашего байндера и вызываем наш метод, который вернет интерфейс сервиса IAppBinder и мы сможем вызывать его методы
//            binder?.let {
//                iMusicPlayerBinder =
//                    (it as MusicPlayerBoundService.MusicPlayerBoundServiceBinder).getMusicPlayerBoundServiceInstance()
//                // В этом месте мы можем заново привязаться, если переводили Bound service в Foreground при закрытии приложения (вызвав наш метод из интерфейса):
//                // iAppBinder?.goToBound()
//            }
//            iMusicPlayerBinder?.stopMediaPlayerAudio() // Останавливаем радио здесь, а не при получении аргументов с предыдущих страниц, т.к. экземпляр binder мы получаем позже и там метод не сработает
//        }
//
//        // этот метод будет вызван, если связь с сервисом была прервана неожиданно
//        override fun onServiceDisconnected(name: ComponentName?) {
//            stopAudio()
//            iMusicPlayerBinder = null
//        }
//    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = LayoutHomeRadioBinding.inflate(inflater, container, false)
        // TOOLBAR
        setHasOptionsMenu(true)
        // TOOLBAR - где будет находиться в нашем layout
        binding?.let {
            appSettings.setToolbar(it.homeToolbar)
        }
        return binding?.root
    }

    // BOUND_SERVICE -> 7. Подпишемся на сервис в нашем фрагменте. Если мы подписываемся на Bound Service в каком-то методе жизненного цикла, мы обязательно должны просчитать точку входа и точку выхода (н-р, если мы входим в методе onStart, то в методе onStop должны отписаться)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        binding?.imagePlay?.setImageResource(R.drawable.play_white)
        binding?.imageStar?.setImageResource(R.drawable.star_transparent)


        // 1.2. ViewModel. We bind our viewModel to the lifecycle of our activity, not fragment. We pass our activity as an owner of the lifecycle.
        // So, we need to do this way:
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]


        // Регистрируем бродкасты, запускаем сервисы
//        activity?.startService(Intent(context, MusicPlayerBoundService::class.java))
//        activity?.registerReceiver(
//            broadcastReceiver,
//            IntentFilter(NOTIFICATION_MUSIC_ACTION_BROADCAST)
//        )
//        activity?.registerReceiver(
//            broadcastReceiverFailures,
//            IntentFilter(MUSIC_PLAYER_SERVICE_FAILURE_PLAYING_BROADCAST)
//        )

//        // BOUND_SERVICE -> 7.1. Запускаем сервис с помощью Intent:
//        requireContext().bindService(
//            Intent(requireContext(), MusicPlayerBoundService::class.java),
//            connection,
//            Context.BIND_AUTO_CREATE
//        )
//        // BIND_AUTO_CREATE - каждый раз, когда мы бандимся, если сервис не был создан, он будет создаваться автоматически
//        // Второй параметр - Service connection. Это объект, внутри которого мы будем получать наш AppServiceBinder (байндер). Здесь не достаточно просто создать экземпляр класса

        // Настройки диалогового окна
        dialogInternetTrouble = Dialog(requireContext())
        // Передайте ссылку на разметку
        dialogInternetTrouble.setContentView(R.layout.layout_internet_trouble_dialog)

        // LOCATION -> 1.4. Получим наш FusedLocationProviderClient. Именно он имеет в себе методы, с помощью которых мы можем определить локацию
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        if (arguments != null) {
            arguments?.getParcelable<RadioStationPresentation>("radio_station") // 2. Получаем радиостанцию из списка на предыдущей странице, если перешли сюда из списка радиостанций
                ?.let { radioStation ->
                    Log.d(TAG, "Выбранный элемент списка: $radioStation")

//                    // ДЛЯ СТАРОГО ПЛЕЙЕРА
//                    viewModel.saveRadioStationAndShow(radioStation, false)
//                    // Так же останавливаем проигрывание из уведомления и обновляем его (возможно, выбрали другую радиостанцию)
//                    context?.let {
//                        CreateNotification.updateNotification(
//                            it,
//                            radioStation,
//                            R.drawable.ic_play_arrow_orange
//                        )
//                    }
//                    // (Останавливаем радио в методе onServiceConnected, а не при получении аргументов с предыдущих страниц, т.к. экземпляр binder мы получаем позже, поэтому здесь метод не сработает)

                    // TODO здесь мы получаем выбранную станцию из списка радиостанций по клику. Необходимо передать её в наш новый плейер
                    mainViewModel.saveNewMediaId(radioStation.url)
                    mainViewModel.fetchSongs(radioStation.countryCode)
                    mainViewModel.playOrToggleSong(radioStation, false)
                    mainViewModel.notJustLaunchedEnableAutoplay()
                }
            arguments?.getParcelable<RadioStationPresentation>("radio_station_favourite")
                ?.let { radioStationFavourite ->
                    Log.d(TAG, "Выбранный элемент списка: $radioStationFavourite")

//                    // ДЛЯ СТАРОГО ПЛЕЙЕРА
//                    viewModel.saveRadioStationAndShow(radioStationFavourite, true)
//                    // Так же останавливаем проигрывание из уведомления и обновляем его (возможно, выбрали другую радиостанцию)
//                    context?.let {
//                        CreateNotification.updateNotification(
//                            it,
//                            radioStationFavourite,
//                            R.drawable.ic_play_arrow_orange
//                        )
//                    }
//                    // (Останавливаем радио в методе onServiceConnected, а не при получении аргументов с предыдущих страниц, т.к. экземпляр binder мы получаем позже, поэтому здесь метод не сработает)

                    // TODO здесь мы получаем выбранную станцию из списка радиостанций по клику. Необходимо передать её в наш новый плейер
                    mainViewModel.saveNewMediaId(radioStationFavourite.url)
                    mainViewModel.fetchSongs(radioStationFavourite.countryCode)
                    mainViewModel.fetchSongs(radioStationFavourite.countryCode)
                    mainViewModel.playOrToggleSong(radioStationFavourite, false)
                    mainViewModel.notJustLaunchedEnableAutoplay()
                }
        } else {
            viewModel.getStoredRadioStation() // 1. Подгрузить радиостанцию из Shared Preference, если она там сохранена. Если нет - текст "выберите радиостанцию"
        }

        // GOOGLE MAPS -> 2.2. Obtain the SupportMapFragment and get notified when the map is ready to be used.
        // Необходимо найти supportFragmentManager в списке всех фрагментов
        // val mapFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.map) не сработает,т.к. этот метод ищет внутри активити.
        // А наш supportFragmentManager находится не в активити: у нас есть HomeRadioFragment, а он - внутри этого HomeRadioFragment (т.е. фрагмент, внутри фрагмента)
        // Если мы открываем фрагмент внутри фрагмента, мы можем искать их с помощью childFragmentManager (заменяем, вместо supportFragmentManager)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        // Т.обр. мы говорим, что слушателем инициализации SupportMapFragment будет именно наш фрагмент (HomeRadioFragment)
        mapFragment.getMapAsync(this)
        // Testing Customize markers
        mapFragment.getMapAsync { googleMap ->
            // ADD MARKERS TO MAP -> 2. Здесь мы добавляем метки городов на карту
//            addMarkers(googleMap)
            // Set custom info window adapter
            googleMap.setInfoWindowAdapter(MarkerInfoWindowAdapter(requireContext()))
            googleMap.uiSettings.isZoomControlsEnabled =
                false // отключаем кнопки по умолчанию, чтобы настроить свои
        }

        // Настраиваем наш customMarker
        customMarkerYouAreHere = Bitmap.createScaledBitmap(
            (ContextCompat.getDrawable(
                requireContext(),
                R.drawable.marker
            ) as BitmapDrawable).bitmap,
            100,
            100,
            false
        )
        customMarkerRadio = Bitmap.createScaledBitmap(
            (ContextCompat.getDrawable(
                requireContext(),
                R.drawable.radio_icon4
            ) as BitmapDrawable).bitmap,
            80,
            80,
            false
        )

        // LOCATION -> 1.5. Создадим метод для получения Current location либо Last location
        getCurrentOrLastLocation()
        // Получить локацию нужно разово, при открытии фрагмента. Обновлять не нужно.

        viewModel.setRecommendedRadioStations()
        initListeners()
        subscribeOnLiveData()

        // COUNTRY LIST MARKERS ON MAP -> 1. Получаем список кодов стран, преобразуем в локальные модели, сохраняем в Room. Будем делать эту работу в foreground service, чтобы отображать уведомление прогресса.
        // !!! Запустить нужно только 1 раз, при запуске программы, затем stopSelf() и больше этот сервис не запускать. Поэтому вызываем сервис из MainActivity

        // COUNTRY LIST MARKERS ON MAP -> 2. Затем подписываемся на локальную БД с помощью CountryListFlow (либо CountryListLiveData)
        subscribeOnFlow()
    }

    private fun initListeners() {
        // Кнопки на карте
        binding?.buttonZoomPlus?.setOnClickListener {
            mMap.animateCamera(CameraUpdateFactory.zoomIn())
        }
        binding?.buttonZoomMinus?.setOnClickListener {
            mMap.animateCamera(CameraUpdateFactory.zoomOut())
        }
        binding?.buttonYouAreHere?.setOnClickListener {
            getCurrentOrLastLocation()
        }
//        binding?.textRadioStationTitle?.setOnClickListener {
//            if (isStationSelected) {
//                if (isPaused) {
//                    playAudio() // <- Нажали кнопку play
//                } else {
//                    stopAudio() // <- Нажали кнопку stop
//                    Toast.makeText(context, "Audio stopped", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//        binding?.imagePlay?.setOnClickListener {
//            if (isStationSelected) {
//                if (isPaused) {
//                    playAudio() // <- Нажали кнопку play
//                } else {
//                    stopAudio() // <- Нажали кнопку stop
//                    Toast.makeText(context, "Audio stopped", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
        binding?.imageStar?.setOnClickListener {
            val currentRadioStation = viewModel.radioStationSavedLiveData.value
            currentRadioStation?.let {
                viewModel.checkIsStationInFavouritesAndChangeTheStar(it)
            }
        }
        binding?.buttonGoToRecommended?.setOnClickListener {
//            stopAudio() // <- Если нажали, перед переходом нужно остановить музыку
            this.findNavController()
                .navigate(R.id.action_homeRadioFragment_to_recommendedListFragment)
        }
        binding?.buttonGoToFavourites?.setOnClickListener {
//            stopAudio() // <- Если нажали, перед переходом нужно остановить музыку
            this.findNavController()
                .navigate(R.id.action_homeRadioFragment_to_favouriteListFragment)
        }
    }

    // Пример инициализации адаптера, из видео
//        private fun setupRecyclerView() = rvAllSongs.apply {
//            adapter = songAdapter
//            layoutManager = LinearLayoutManager(requireContext())
//        }

    private fun subscribeOnLiveData() {
        // Показываем или прячем Progress
        viewModel.showProgressLiveData.observe(viewLifecycleOwner, {
            showProgress()
        })
        viewModel.hideProgressLiveData.observe(viewLifecycleOwner, {
            hideProgress()
        })
        viewModel.dialogInternetTroubleLiveData.observe(viewLifecycleOwner, {
            dialogInternetTrouble.show()
        })
        viewModel.failedLiveData.observe(viewLifecycleOwner, {
            Toast.makeText(context, "Failure. Something went wrong", Toast.LENGTH_LONG).show()
        })
        viewModel.radioStationSavedLiveData.observe(
            viewLifecycleOwner,
            { radioStationPresentation ->
//                binding?.textRadioStationTitle?.text = radioStationPresentation.stationName
                isStationSelected = true
            })
        viewModel.stationSavedInFavouritesLiveData.observe(viewLifecycleOwner, {
            binding?.imageStar?.setImageResource(R.drawable.star)
        })
        viewModel.stationDeletedFromFavouritesLiveData.observe(viewLifecycleOwner, {
            binding?.imageStar?.setImageResource(R.drawable.star_transparent)
        })


        // Subscribe to mediaItems LiveData
        // As result we have here List<RadioStationPresentation>, surrounded by Resource (Resource<List<RadioStationPresentation>>)
        // That's why we can easily check the state of our current list of stations
        mainViewModel.mediaItemsListLiveData.observe(viewLifecycleOwner) { result ->
            when (result.status) {
                Status.SUCCESS -> {
                    binding?.progressCircular?.isVisible = false
                    // Здесь можно заполнить наш адаптер для recycler view, если он есть на этой странице.
//                    result.data?.let { songs ->
//                        songAdapter.songs = songs // это триггернет setter из адаптера: var songs: List<Song>; get() = differ.currentList; !!! set(value) = differ.submitList(value) !!!
//                    }
                }
                Status.ERROR -> Unit // We never emitted here an error status, so we don't do anything here
                Status.LOADING -> binding?.progressCircular?.isVisible = true
            }
        }
    }

    private fun subscribeOnFlow() {
        lifecycleScope.launchWhenCreated {
            viewModel.countryListFlow.collect { countryPresentationList ->
                binding?.textLoadingData?.isVisible = false
                countryList =
                    countryPresentationList // Заполним массив для последующей обработки клика
                showProgress()
                countryPresentationList.forEach { countryPresentation ->
                    addMarkersOnMap(countryPresentation)
                }
                hideProgress()

                if (countryPresentationList == emptyList<CountryPresentation>()) { // Мы переходим на эту страницу только если БД не пуста. Если массив пустой - что-то пошло не так
                    showProgress()
                    Toast.makeText(
                        context,
                        "Something went wrong. Waiting for database response.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

//    override fun playAudio() {
//        // Изменяем уведомление и включаем радио
//        val radioStationSaved = viewModel.radioStationSavedLiveData.value
//        radioStationSaved?.let {
//            iMusicPlayerBinder?.playMediaPlayerAudioAndShowNotification(it)
//        }
//
//        binding?.imagePlay?.setImageResource(R.drawable.pause_white)
//        isPaused = false
//    }

//    override fun stopAudio() {
//        // Останавливаем проигрывание
//        iMusicPlayerBinder?.stopMediaPlayerAudio()
//        // Изменяем уведомление
//        val radioStationSaved = viewModel.radioStationSavedLiveData.value
//        radioStationSaved?.let {
//            iMusicPlayerBinder?.stopMediaPlayerNotification(it)
//        }
//        binding?.imagePlay?.setImageResource(R.drawable.play_white)
//        isPaused = true
//    }

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
    private fun getCurrentOrLastLocation() {
        // Last location
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            location
                ?: return@addOnSuccessListener // Т.к. location может прилететь null, пишем тернарный оператор. Если location = null, то просто выходим из метода addOnSuccessListener
            Log.d(
                TAG,
                "LastLocation: latitude = ${location.latitude}, longitude = ${location.longitude}"
            ) // Проверяем получение ширины и долготы в логе

            // GOOGLE MAPS -> 2.4. Покажем на карте, где мы находимся (один раз). Создадим метод showMyLocation() и передадим туда текущее местоположение
            showMyLocation(LatLng(location.latitude, location.longitude))
        }

        // Current location
        fusedLocationProviderClient.getCurrentLocation(
            // PRIORITY_NO_POWER - незначительное влияние на энергопотребление + получение обновлений местоположений, когда они доступны.
            // С этим параметром приложение не инициирует никаких обновлений местоположения, но получает местоположения, инициированные другими приложениями.
            LocationRequest.PRIORITY_NO_POWER,
            object : CancellationToken() {
                // Создадим переменную со значением по умолчанию
                private var isCancellationRequested = false

                // Если запрошено
                override fun onCanceledRequested(p0: OnTokenCanceledListener): CancellationToken {
                    isCancellationRequested = true
                    return this
                }

                // Проверка статуса
                override fun isCancellationRequested(): Boolean {
                    return isCancellationRequested
                }
            }).addOnSuccessListener { location: Location? ->
            // Т.к. location может прилететь null, пишем тернарный оператор. Если location = null, то просто выходим из метода addOnSuccessListener
            location ?: return@addOnSuccessListener
            // Проверяем получение ширины и долготы в логе
            Log.d(
                TAG,
                "CurrentLocation: latitude = ${location.latitude}, longitude = ${location.longitude}"
            )

            // GOOGLE MAPS -> 2.4. Покажем на карте, где мы находимся (один раз). Создадим метод showMyLocation() и передадим туда текущее местоположение
            showMyLocation(LatLng(location.latitude, location.longitude))
        }
    }

    // GOOGLE MAPS -> 2.5. Покажем на карте, где мы находимся. Создадим метод showMyLocation() и передадим туда текущее местоположение
    private fun showMyLocation(latLng: LatLng) {
        Log.d(
            TAG,
            "Метод showMyLocation вызван: latitude = ${latLng.latitude}, longitude = ${latLng.longitude}"
        )
        // Настраиваем маркер, если он не null
        customMarkerYouAreHere?.let { customBitmapMarker ->
            marker = mMap.addMarker(
                MarkerOptions()
                    .title("You are here")
                    .snippet(null)
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.fromBitmap(customBitmapMarker))
            )
        }
        // И передвинем камеру
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 5f))
    }

    // ADD MARKERS TO MAP -> 3. Здесь мы добавляем метки городов на карту
    // Adds marker representations of the places list on the provided GoogleMap object
//    private fun addMarkers(googleMap: GoogleMap) {
//        places.forEach { place ->
//            customMarkerRadio?.let { customBitmapMarker ->
//                val marker = googleMap.addMarker(
//                    MarkerOptions()
//                        .title(place.name)
//                        .snippet("Открыть список радиостанций")
//                        .position(place.latLng)
//                        .icon(BitmapDescriptorFactory.fromBitmap(customBitmapMarker))
//                )
//            }
//
//            // Set place as the tag on the marker object so it can be referenced within
//            // MarkerInfoWindowAdapter
//            marker?.tag = place
//        }
//    }

    private fun addMarkersOnMap(countryPresentation: CountryPresentation) {
        Log.d(
            TAG,
            "Метод addMarkersOnMap вызван: страна = ${countryPresentation.countryName}"
        )
        customMarkerRadio?.let { customBitmapMarker ->
            val marker = mMap.addMarker(
                MarkerOptions()
                    .title(countryPresentation.countryName)
                    .snippet("Список радиостанций (${countryPresentation.stationCount})")
                    .position(countryPresentation.countryLocation)
                    .icon(BitmapDescriptorFactory.fromBitmap(customBitmapMarker))
            )

            // Set place as the tag on the marker object so it can be referenced within
            // MarkerInfoWindowAdapter
            marker?.tag = countryPresentation
        }
    }

    // GOOGLE MAPS -> 2.3. Сюда прилетит карта, когда она будет готова к работе.
    // Когда она сюда залетит, мы сможем к ней обратиться и сказать, что она будет равна нашей переменной класса
    // !!! This method passes a GoogleMap instance to you, which you can then use to perform various operations on the map.
    override fun onMapReady(map: GoogleMap) {
        this.mMap = map
        // Далее по документации здесь делают некоторые действия, однако мы сделаем их в отдельном методе

        // Обработка клика по InfoWindow маркера
        mMap.setOnInfoWindowClickListener { marker ->
            val latLon = marker.position

            // Cycle through countryList array
            for (country in countryList) {
                if (latLon == country.countryLocation) {
                    //match found!  Do something....

//                    // Если нажали на маркер, перед переходом на список нужно остановить музыку
//                    stopAudio()

                    Log.d(
                        TAG,
                        "Результат - выбран маркер: $latLon = ${country.countryLocation}, ${country.countryName}"
                    )

                    // Перенесём countryCode на RadioListFragment для запроса списка станций
                    val direction =
                        HomeRadioFragmentDirections.actionHomeRadioFragmentToRadioListFragment("${country.countryCode}||${country.countryName}")
                    if (this.findNavController().currentDestination?.id == R.id.homeRadioFragment) {
                        this.findNavController().navigate(direction)
                    }
//                    this.findNavController().navigate(direction) - при переходе на Канаду - ошибка. Помогла проверка (см. выше)

                    if (country.countryCode != mainViewModel.curPlayingSongLiveData.value?.description?.subtitle){
                        mainViewModel.fetchSongs(country.countryCode)
                    }

                    Toast.makeText(
                        context,
                        "Asking server for the radio station list...",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // TOOLBAR
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.home_toolbar_menu, menu)
    }

    // TOOLBAR - обработка клика
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.log_out -> {
            showLogoutDialog()
            Log.d(TAG, "showLogoutDialog() was called")
            true
        }
        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            Log.d(TAG, "else result")
            super.onOptionsItemSelected(item)
        }
    }

    // TOOLBAR - Описываем метод из интерфейса ILogOutListener для выхода из аккаунта приложения
    override fun onLogOut() {
//        stopAudio() // Если нажали, перед переходом нужно остановить музыку
        viewModel.logout()
        activity?.finish()
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

//    override fun onDestroy() {
        // MUSIC PLAYER ON NOTIFICATION -> END. Запускали сервис - убираем уведомления. Регистрировали бродкаст - отписываемся
//        notificationManager.cancelAll()
//        activity?.unregisterReceiver(broadcastReceiver)
//        activity?.unregisterReceiver(broadcastReceiverFailures)

//        // BOUND_SERVICE -> 7.2. Заканчиваем соединение. Сюда также передаём наш Service connection. Создадим его (см. выше)
//        iMusicPlayerBinder?.let {
//            requireContext().unbindService(connection)
//        }
//        super.onDestroy()
//    }

//    // MUSIC PLAYER ON NOTIFICATION -> 8. Receiving Broadcast
//    private var broadcastReceiver: BroadcastReceiver? = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent) {
//
//            // Describe different situations, such as prev track, play, next track
//            when (intent.getStringExtra("action_name")) {
//                Constants.NOTIFICATION_MUSIC_ACTION_PLAY -> if (isPaused) {
//                    playAudio() // <- Нажали кнопку play
//                } else {
//                    stopAudio() // <- Нажали кнопку stop
//                }
//                else -> Log.d(TAG, "Wrong action")
//            }
//        }
//    }

//    // Если не получилось запустить радиостанцию методом playMediaPlayerAudioAndShowNotification(), нужно изменить кнопочку обратно на паузу. Получаем intent из MusicPlayerBoundService
//    private var broadcastReceiverFailures: BroadcastReceiver? = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent) {
//            binding?.imagePlay?.setImageResource(R.drawable.play_white)
//            isPaused = intent.getBooleanExtra("play_failure", true)
//        }
//    }
}