package com.myproject.radiojourney.presentation.content.homeRadio

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
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
import com.myproject.radiojourney.model.presentation.CountryPresentation
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.myproject.radiojourney.model.presentation.RadioStationPresentation
import android.widget.Toast
import com.myproject.radiojourney.databinding.LayoutHomeRadioBinding
import com.myproject.radiojourney.model.presentation.RadioStationFavouritePresentation
import kotlinx.coroutines.*
import java.io.IOException
import java.lang.Exception

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
 */
@AndroidEntryPoint
class HomeRadioFragment : BaseContentFragmentAbstract(), OnMapReadyCallback {
    companion object {
        private const val TAG = "HomeRadioFragment"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: LayoutHomeRadioBinding? = null

    @Inject
    lateinit var appSettings: IAppSettings

    private val viewModel by viewModels<HomeRadioViewModel>()
    private lateinit var dialogInternetTrouble: Dialog
    private var isPaused = true

    // PLAY URL (MP3), MEDIA PLAYER -> 1. Создаём переменные
    // MediaPlayer – класс, который позволит вам проигрывать аудио/видео файлы с возможностью сделать паузу и перемотать в нужную позицию.
    // MediaPlayer умеет работать с различными источниками, это может быть: путь к файлу (на SD или в инете), адрес потока, Uri или файл из папки res/raw.
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioUrl: String = ""
    private var isStationSelected = false

    // Переменная для нашего FusedLocationProviderClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // GOOGLE MAPS -> 2.1. Объявляем переменную, в соответствии с инструкцией от гугла
    private lateinit var mMap: GoogleMap

    // GOOGLE MAPS -> 3.6. Объявляем переменную для маркера
    private var marker: Marker? = null
    private var customMarkerYouAreHere: Bitmap? = null
    private var customMarkerRadio: Bitmap? = null

    //    // ADD MARKERS TO MAP -> 1. Для примера, сейчас. Потом подгружать список по запросу
//    private val places: List<Place> = listOf(
//        Place(name = "Minsk", latLng = LatLng(53.90580039557321, 27.562806971874416))
//    )
    private var countryList = listOf<CountryPresentation>()

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.imagePlay?.setImageResource(R.drawable.play_white)
        binding?.imageStar?.setImageResource(R.drawable.star_transparent)
//        progressHorizontal = view.findViewById(R.id.progress_horizontal)

        // Настройки диалогового окна
        dialogInternetTrouble = Dialog(requireContext())
        // Передайте ссылку на разметку
        dialogInternetTrouble.setContentView(R.layout.layout_internet_trouble_dialog)

        // LOCATION -> 1.4. Получим наш FusedLocationProviderClient. Именно он имеет в себе методы, с помощью которых мы можем определить локацию
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        // PLAY URL (MP3), MEDIA PLAYER -> 2. Получаем AudioManager
        audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (arguments != null) {
            // 2. Получаем радиостанцию из списка на предыдущей странице, если перешли сюда из списка радиостанций
            arguments?.getParcelable<RadioStationPresentation>("radio_station")
                ?.let { radioStation ->
                    Log.d(TAG, "Выбранный элемент списка: $radioStation")
                    viewModel.saveRadioStationAndShow(radioStation)
                }
            arguments?.getParcelable<RadioStationFavouritePresentation>("radio_station_favourite")
                ?.let { radioStationFavourite ->
                    Log.d(TAG, "Выбранный элемент списка: $radioStationFavourite")
                    viewModel.saveFavouriteRadioStationAndShow(radioStationFavourite)
                }
        } else {
            // 1. Подгрузить радиостанцию из Shared Preference, если она там сохранена. Если нет - текст "выберите радиостанцию"
            viewModel.getStoredRadioStation()
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

        initListeners()
        subscribeOnLiveData()

        // COUNTRY LIST MARKERS ON MAP -> 1. Получаем список кодов стран, преобразуем в локальные модели, сохраняем в Room.
        // Будем делать эту работу в foreground service, чтобы отображать уведомление прогресса.
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
        binding?.textRadioStationTitle?.setOnClickListener {
            if (isStationSelected) {
                if (isPaused) {
                    // Нажали кнопку play
                    playAudio()
                } else {
                    // Нажали кнопку stop
                    stopAudio()
                    Toast.makeText(context, "Audio stopped", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding?.imagePlay?.setOnClickListener {
            if (isStationSelected) {
                if (isPaused) {
                    // Нажали кнопку play
                    playAudio()
                } else {
                    // Нажали кнопку stop
                    stopAudio()
                    Toast.makeText(context, "Audio stopped", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding?.imageStar?.setOnClickListener {
            val currentRadioStation = viewModel.radioStationSavedLiveData.value
            currentRadioStation?.let {
                viewModel.checkIsStationInFavouritesAndChangeTheStar(it)
            }
        }
        binding?.buttonAddToFavourites?.setOnClickListener {
            if (isStationSelected) {
                val currentRadioStation = viewModel.radioStationSavedLiveData.value
                currentRadioStation?.let {
                    viewModel.addStationToFavourites(it)
                }
            }
        }
        binding?.buttonGoToFavourites?.setOnClickListener {
            // Если нажали, перед переходом нужно остановить музыку
            stopAudio()

            this.findNavController()
                .navigate(R.id.action_homeRadioFragment_to_favouriteListFragment)
        }
    }

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
        viewModel.radioStationSavedLiveData.observe(
            viewLifecycleOwner,
            { radioStationPresentation ->
                binding?.textRadioStationTitle?.text = radioStationPresentation.stationName
                audioUrl = radioStationPresentation.url
                isStationSelected = true
            })
        viewModel.addStationToFavouritesFailedLiveData.observe(viewLifecycleOwner, {
            Toast.makeText(
                context,
                "Interacting with favourites failed. Smth wrong with your token. Try re-login.",
                Toast.LENGTH_LONG
            ).show()
        })
        viewModel.failedLiveData.observe(viewLifecycleOwner, {
            Toast.makeText(context, "Failure. Something went wrong", Toast.LENGTH_LONG).show()
        })
        viewModel.stationSavedInFavouritesLiveData.observe(viewLifecycleOwner, {
            binding?.imageStar?.setImageResource(R.drawable.star)
        })
        viewModel.stationDeletedFromFavouritesLiveData.observe(viewLifecycleOwner, {
            binding?.imageStar?.setImageResource(R.drawable.star_transparent)
        })
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

                if (countryPresentationList == emptyList<CountryPresentation>()) {
                    showProgress()
                    Toast.makeText(
                        context,
                        "Cashing. Please, wait.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // PLAY URL (MP3), MEDIA PLAYER -> 3. Метод для запуска проигрывания.
    private fun playAudio() {
        // Сначала мы освобождаем ресурсы текущего проигрывателя.
        releaseMediaPlayer()

        // Затем стартуем проигрывание.
        Toast.makeText(context, "Connecting to radio station...", Toast.LENGTH_SHORT).show()
        binding?.imagePlay?.setImageResource(R.drawable.pause_white)
        isPaused = false

        try {
            Log.d(TAG, "PLAY URL (MP3), MEDIA PLAYER -> start playing HTTP")
            mediaPlayer = MediaPlayer().apply {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                } else {
                    setAudioStreamType(AudioManager.STREAM_MUSIC)
                }
                // setAudioStreamType – задает аудио-поток, который будет использован для проигрывания.
                // Их существует несколько: STREAM_MUSIC, STREAM_NOTIFICATION и п.
                // Предполагаю, что созданы они для того, чтобы можно было задавать разные уровни громкости, например, играм, звонкам и уведомлениям.
                // Этот метод можно и пропустить, если вам не надо явно указывать какой-то поток. Насколько я понял, по умолчанию используется STREAM_MUSIC.

                reset()

                try {
                    setDataSource(audioUrl)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                // Далее используется метод prepare или prepareAsync (в паре с OnPreparedListener).
                // Эти методы подготавливают плеер к проигрыванию. И, как понятно из названия, prepareAsync делает это асинхронно,
                // и, когда все сделает, сообщит об этом слушателю из метода setOnPreparedListener.
                // А метод prepare работает синхронно. Соотвественно, если хотим прослушать файл из инета, то используем prepareAsync,
                // иначе наше приложение повесится, т.к. заблокируется основной поток, который обслуживает UI.
                Log.d(TAG, "PLAY URL (MP3), MEDIA PLAYER -> prepareAsync")
                setOnPreparedListener {
                    Log.d(TAG, "PLAY URL (MP3), MEDIA PLAYER -> onPrepared")
                    it.start() // Метод start запускает проигрывание
                    Toast.makeText(context, "Audio started playing", Toast.LENGTH_SHORT).show()
                }
                prepareAsync() // might take long! (for buffering, etc)
                setOnErrorListener { _, what, extra ->
                    Toast.makeText(context, "Failed to connect.", Toast.LENGTH_SHORT).show()
                    stopAudio()
                    Log.d(TAG, "PLAY URL (MP3), MEDIA PLAYER -> setOnErrorListener $what $extra")
                    true
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Failed to connect. Try to click \"play\" or select another station",
                Toast.LENGTH_SHORT
            ).show()
            stopAudio()
        }

        if (mediaPlayer == null) return
    }

    // PLAY URL (MP3), MEDIA PLAYER -> 4. В методе releaseMP мы выполняем метод release.
    // Он освобождает используемые проигрывателем ресурсы, его рекомендуется вызывать когда вы закончили работу с плеером.
    // Более того, хелп рекомендует вызывать этот метод и при onPause/onStop, если нет острой необходимости держать объект.
    private fun releaseMediaPlayer() {

        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    // PLAY URL (MP3), MEDIA PLAYER -> 5. Метод для остановки проигрывания
    private fun stopAudio() {
        binding?.imagePlay?.setImageResource(R.drawable.play_white)
        isPaused = true

        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop() // Останавливает проигрывание
            }
        }
        releaseMediaPlayer()
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

                    // Если нажали на маркер, перед переходом на список нужно остановить музыку
                    stopAudio()

                    Log.d(
                        TAG,
                        "Результат - выбран маркер: $latLon = ${country.countryLocation}, ${country.countryName}"
                    )

                    // Перенесём countryCode на RadioListFragment для запроса списка станций
                    val direction =
                        HomeRadioFragmentDirections.actionHomeRadioFragmentToRadioListFragment("${country.countryCode}||${country.countryName}")
                    this.findNavController().navigate(direction)
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
        // Если нажали, перед переходом нужно остановить музыку
        stopAudio()

        viewModel.logout()
        this.findNavController().navigate(R.id.action_homeRadioFragment_to_auth_nav_graph)
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    // PLAY URL (MP3), MEDIA PLAYER -> 6. В методе onDestroy обязательно освобождаем ресурсы проигрывателя
    override fun onDestroy() {
//        context?.unregisterReceiver(myBroadcastReceiver)
        releaseMediaPlayer()
        super.onDestroy()
    }
}