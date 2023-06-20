package com.myproject.radiojourney.presentation.content.homeRadio

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
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
import javax.inject.Inject
import com.myproject.radiojourney.entities.presentation.CountryPresentation
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import com.myproject.radiojourney.databinding.LayoutHomeRadioBinding
import com.myproject.radiojourney.utils.oldMusicPlayer.*
import kotlinx.coroutines.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
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
 * GOOGLE MAPS -> 2. В инструкции от google всё делается в activity, а у нас - фрагмент. Следовательно, будут небольшие изменения
 *
 * TODO повторить проверку разрешения определения местоположения
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

    // 1.1. ViewModel. We bind our viewModel to the cycle of our activity, not fragment. So, we need to do this way:
    private lateinit var mainViewModel: MainViewModel

    private val viewModel by viewModels<HomeRadioViewModel>()

    private lateinit var dialogInternetTrouble: Dialog

    // Переменная для нашего FusedLocationProviderClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // GOOGLE MAPS -> 2.1. Объявляем переменную, в соответствии с инструкцией от google
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = LayoutHomeRadioBinding.inflate(inflater, container, false)
        // TOOLBAR
//        setHasOptionsMenu(true) // setHasOptionsMenu deprecated
        // TOOLBAR - где будет находиться в нашем layout
        binding?.let {
            appSettings.setToolbar(it.homeToolbar)
        }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TOOLBAR in TIRAMISU
        // The usage of an interface lets you inject your own implementation
        val menuHost: MenuHost = requireActivity()

        // Add menu items without using the Fragment Menu APIs
        // Note how we can tie the MenuProvider to the viewLifecycleOwner
        // and an optional Lifecycle.State (here, RESUMED) to indicate when
        // the menu should be visible
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.home_toolbar_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.log_out -> {
                        showLogoutDialog()
                        Log.d(TAG, "showLogoutDialog() was called")
                        true
                    }
                    else -> {
                        // If we got here, the user's action was not recognized.
                        Log.d(TAG, "else result")
                        false
                    }
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        // Если каким-то образом мы попали на этот фрагмент минуя первый, загрузочный фрагмент - стоит ещё раз проверить разрешения
        // Если разрешения нет - или запросить их, или перекинуть на загрузочный фрагмент и там запросить
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissionsMap ->
                if (permissionsMap[Manifest.permission.ACCESS_COARSE_LOCATION] != true
                    &&
                    permissionsMap[Manifest.permission.ACCESS_FINE_LOCATION] != true
                ) {
                    Toast.makeText(
                        requireContext(),
                        "We can't show your location without an access to it",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }

        // 1.2. ViewModel. We bind our viewModel to the lifecycle of our activity, not fragment. We pass our activity as an owner of the lifecycle.
        // So, we need to do this way:
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        // Настройки диалогового окна
        dialogInternetTrouble = Dialog(requireContext())
        // Передайте ссылку на разметку
        dialogInternetTrouble.setContentView(R.layout.layout_internet_trouble_dialog)

        // LOCATION -> 1.4. Получим наш FusedLocationProviderClient. Именно он имеет в себе методы, с помощью которых мы можем определить локацию
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        if (arguments != null) {

            mainViewModel.showProgressAndDisableClick()

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                arguments?.getParcelable("radio_station", RadioStationPresentation::class.java) // 2. Получаем радиостанцию из списка на предыдущей странице, если перешли сюда из списка радиостанций
//                    ?.let { radioStation ->
//                        Log.d(TAG, "Выбранный элемент списка: $radioStation")
//
//                        // Здесь мы получаем выбранную станцию из списка радиостанций по клику. Необходимо передать её в наш новый плейер
//                        mainViewModel.saveNewMediaId(radioStation.urlResolved)
//                        mainViewModel.fetchSongs(radioStation.countryCode)
//                        mainViewModel.playOrToggleSong(radioStation, false)
//                        mainViewModel.notJustLaunchedEnableAutoplay()
//                    }
//            } else {
//                arguments?.getParcelable<RadioStationPresentation>("radio_station") // 2. Получаем радиостанцию из списка на предыдущей странице, если перешли сюда из списка радиостанций
//                    ?.let { radioStation ->
//                        Log.d(TAG, "Выбранный элемент списка: $radioStation")
//
//                        // Здесь мы получаем выбранную станцию из списка радиостанций по клику. Необходимо передать её в наш новый плейер
//                        mainViewModel.saveNewMediaId(radioStation.urlResolved)
//                        mainViewModel.fetchSongs(radioStation.countryCode)
//                        mainViewModel.playOrToggleSong(radioStation, false)
//                        mainViewModel.notJustLaunchedEnableAutoplay()
//                    }
//            }
//
// arguments?.getParcelable<RadioStationPresentation>("radio_station") is deprecated. For lesser code, let's write inline lambda for < and >= Build.VERSION_CODES.TIRAMISU
//
//            arguments?.getParcelable<RadioStationPresentation>("radio_station") // 2. Получаем радиостанцию из списка на предыдущей странице, если перешли сюда из списка радиостанций
//                ?.let { radioStation ->
//                    Log.d(TAG, "Выбранный элемент списка: $radioStation")
//
//                    // Здесь мы получаем выбранную станцию из списка радиостанций по клику. Необходимо передать её в наш новый плейер
//                    mainViewModel.saveNewMediaId(radioStation.urlResolved)
//                    mainViewModel.fetchSongs(radioStation.countryCode)
//                    mainViewModel.playOrToggleSong(radioStation, false)
//                    mainViewModel.notJustLaunchedEnableAutoplay()
//                }

            arguments?.parcelable<RadioStationPresentation>("radio_station") // 2. Получаем радиостанцию из списка на предыдущей странице, если перешли сюда из списка радиостанций
                ?.let { radioStation ->
                    Log.d(TAG, "Выбранный элемент списка: $radioStation")

                    // Здесь мы получаем выбранную станцию из списка радиостанций по клику. Необходимо передать её в наш новый плейер
                    mainViewModel.saveNewMediaId(radioStation.stationuuid)
                    mainViewModel.fetchSongs(radioStation.countryCode)
                    mainViewModel.playOrToggleSong(radioStation, false)
                    mainViewModel.notJustLaunchedEnableAutoplay()
                }

//            arguments?.getParcelable<RadioStationPresentation>("radio_station_favourite") // 2. Получаем радиостанцию из списка на предыдущей странице, если перешли сюда из списка радиостанций
//                ?.let { radioStationFavourite ->
//                    Log.d(TAG, "Выбранный элемент списка: $radioStationFavourite")
//
//                    // Здесь мы переходим из фрагмента "Избранное". Стоит загрузить в плейер плейлист избранного.
//                    mainViewModel.saveNewMediaId(radioStationFavourite.urlResolved)
//                    mainViewModel.fetchSongs("FAV")
//                    mainViewModel.playOrToggleSong(radioStationFavourite, false)
//                    mainViewModel.notJustLaunchedEnableAutoplay()
//                }

            arguments?.parcelable<RadioStationPresentation>("radio_station_favourite") // 2. Получаем радиостанцию из списка на предыдущей странице, если перешли сюда из списка радиостанций
                ?.let { radioStationFavourite ->
                    Log.d(TAG, "Выбранный элемент списка: $radioStationFavourite")

                    // Здесь мы переходим из фрагмента "Избранное". Стоит загрузить в плейер плейлист избранного.
                    mainViewModel.saveNewMediaId(radioStationFavourite.stationuuid)
                    mainViewModel.fetchSongs("FAV")
                    mainViewModel.playOrToggleSong(radioStationFavourite, false)
                    mainViewModel.notJustLaunchedEnableAutoplay()
                }

//            arguments?.getParcelable<RadioStationPresentation>("radio_station_favourite") // 2. Получаем радиостанцию из списка на предыдущей странице, если перешли сюда из списка радиостанций
//                ?.let { radioStationFavourite ->
//                    Log.d(TAG, "Выбранный элемент списка: $radioStationFavourite")
//
//                    // Здесь мы переходим из фрагмента "Избранное". Стоит загрузить в плейер плейлист избранного.
//                    mainViewModel.saveNewMediaId(radioStationFavourite.urlResolved)
//                    mainViewModel.fetchSongs("FAV")
//                    mainViewModel.playOrToggleSong(radioStationFavourite, false)
//                    mainViewModel.notJustLaunchedEnableAutoplay()
//                }

            arguments?.parcelable<RadioStationPresentation>("radio_station_favourite") // 2. Получаем радиостанцию из списка на предыдущей странице, если перешли сюда из списка радиостанций
                ?.let { radioStationFavourite ->
                    Log.d(TAG, "Выбранный элемент списка: $radioStationFavourite")

                    // Здесь мы переходим из фрагмента "Избранное". Стоит загрузить в плейер плейлист избранного.
                    mainViewModel.saveNewMediaId(radioStationFavourite.stationuuid)
                    mainViewModel.fetchSongs("FAV")
                    mainViewModel.playOrToggleSong(radioStationFavourite, false)
                    mainViewModel.notJustLaunchedEnableAutoplay()
                }

        }

//        else {
//            viewModel.getStoredRadioStation() // 1. Подгрузить радиостанцию из Shared Preference, если она там сохранена. Если нет - текст "выберите радиостанцию"
//        }

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
//            addMarkers(googleMap)
            // Set custom info window adapter
            googleMap.setInfoWindowAdapter(MarkerInfoWindowAdapter(requireContext()))
            googleMap.uiSettings.isZoomControlsEnabled =
                false // отключаем кнопки по умолчанию, чтобы настроить свои
        }

        // Настраиваем наш customMarker
        var myMarkerBitmap: Bitmap? = null

        if (SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            context?.let { nonNullContext ->
                AppCompatResources.getDrawable(
                    nonNullContext,
                    R.drawable.ic_baseline_location_on_24_orange
                )?.let {
                    myMarkerBitmap = viewModel.bitmapOrNull(it)

                    myMarkerBitmap?.let { nonNullBitmap ->
                        customMarkerYouAreHere = Bitmap.createScaledBitmap(
                            nonNullBitmap,
                            100,
                            100,
                            false
                        )
                    }
                }
            }

//            context?.getDrawable(R.drawable.ic_baseline_location_on_24_orange)?.let {
//                myMarkerBitmap = viewModel.bitmapOrNull(it)
//
//                myMarkerBitmap?.let { nonNullBitmap ->
//                    customMarkerYouAreHere = Bitmap.createScaledBitmap(
//                        nonNullBitmap,
//                        100,
//                        100,
//                        false
//                    )
//                }
//            }

        }

        if (SDK_INT < Build.VERSION_CODES.LOLLIPOP || myMarkerBitmap == null) {
            customMarkerYouAreHere = Bitmap.createScaledBitmap(
                (ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.marker
                ) as BitmapDrawable).bitmap,
                100,
                100,
                false
            )
        }

        // Настраиваем radio icon
        var myRadioBitmap: Bitmap? = null

        if (SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            context?.let { nonNullContext ->
                AppCompatResources.getDrawable(
                    nonNullContext,
                    R.drawable.ic_baseline_radio_24_orange
                )?.let {
                    myRadioBitmap = viewModel.bitmapOrNull(it)

                    myRadioBitmap?.let { nonNullBitmap ->
                        customMarkerRadio = Bitmap.createScaledBitmap(
                            nonNullBitmap,
                            80,
                            80,
                            false
                        )
                    }
                }
            }

//            context?.getDrawable(R.drawable.ic_baseline_radio_24_orange)?.let {
//                myRadioBitmap = viewModel.bitmapOrNull(it)
//
//                myRadioBitmap?.let { nonNullBitmap ->
//                    customMarkerRadio = Bitmap.createScaledBitmap(
//                        nonNullBitmap,
//                        80,
//                        80,
//                        false
//                    )
//                }
//            }
        }

        if (SDK_INT < Build.VERSION_CODES.LOLLIPOP || myRadioBitmap == null) {
            customMarkerRadio = Bitmap.createScaledBitmap(
                (ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.radio_icon4
                ) as BitmapDrawable).bitmap,
                80,
                80,
                false
            )
        }

        // LOCATION -> 1.5. Создадим метод для получения Current location либо Last location
        getCurrentOrLastLocation()
        // Получить локацию нужно разово, при открытии фрагмента. Обновлять не нужно.

        binding?.buttonYouAreHere?.setOnClickListener {
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
                getCurrentOrLastLocation()
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

//        viewModel.setRecommendedRadioStations()
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
        binding?.imageSettings?.setOnClickListener {
            if (this.findNavController().currentDestination?.id == R.id.homeRadioFragment) {
                this.findNavController()
                    .navigate(R.id.action_homeRadioFragment_to_settingsFragment)
            }
        }
        binding?.buttonGoToFavourites?.setOnClickListener {
            if (this.findNavController().currentDestination?.id == R.id.homeRadioFragment) {
                this.findNavController()
                    .navigate(R.id.action_homeRadioFragment_to_favouriteListFragment)
            }
        }
    }

    // Пример инициализации адаптера, из видео
//        private fun setupRecyclerView() = rvAllSongs.apply {
//            adapter = songAdapter
//            layoutManager = LinearLayoutManager(requireContext())
//        }

    private fun subscribeOnLiveData() {
        // Показываем или прячем Progress
        viewModel.showProgressLiveData.observe(viewLifecycleOwner) {
            showProgress()
        }
        viewModel.hideProgressLiveData.observe(viewLifecycleOwner) {
            hideProgress()
        }
        mainViewModel.setNonClickableLiveData.observe(viewLifecycleOwner) {
            // Изредка не срабатывает логика и кнопки остаются заблокированым. В таком случае нет возможности продолжать пользоваться приложением.
//            // Запустить отображение прогресс бара + заблокировать нажатия как на HomeRadioFragment, так и проигрыватель в main activity
//            // HomeRadioFragment
            binding?.buttonGoToFavourites?.isClickable = false
            binding?.buttonGoToFavourites?.isEnabled = false
//            // TODO карта - не проработано (InfoWindow)

            // Progress bar
            showProgress()
            binding?.progressCircularLoadingArguments?.isVisible = true
        }
        mainViewModel.setClickableLiveData.observe(viewLifecycleOwner) {
//            // Убрать отображение прогресс бара + разблокировать нажатия как на HomeRadioFragment, так и проигрыватель в main activity
//            // HomeRadioFragment
            binding?.buttonGoToFavourites?.isClickable = true
            binding?.buttonGoToFavourites?.isEnabled = true
//            // TODO карта - не проработано (InfoWindow)

            // Progress bar
            hideProgress()
            binding?.progressCircularLoadingArguments?.isVisible = false
        }
        mainViewModel.dialogInternetTroubleLiveData.observe(viewLifecycleOwner) {
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
        // Subscribe to mediaItems LiveData
        // As result we have here List<RadioStationPresentation>, surrounded by Resource (Resource<List<RadioStationPresentation>>)
        // That's why we can easily check the state of our current list of stations
        mainViewModel.mediaItemsListLiveData.observe(viewLifecycleOwner) { result ->
            when (result.status) {
                Status.SUCCESS -> {
                    binding?.progressCircular?.isVisible = false
                    // Здесь можно заполнить наш адаптер для recycler view, если он есть на этой странице.
//                    result.data?.let { songs ->
//                        songAdapter.songs = songs // это триггернёт setter из адаптера: var songs: List<Song>; get() = differ.currentList; !!! set(value) = differ.submitList(value) !!!
//                    }
                }

                Status.ERROR -> Unit // We never emitted here an error status, so we don't do anything here
                Status.LOADING -> binding?.progressCircular?.isVisible = true
            }
        }
    }

    private fun subscribeOnFlow() {
        // Function launchWhenCreated is deprecated as it can lead to wasted resources in some cases.
        // Replace with suspending repeatOnLifecycle to run the block whenever the Lifecycle state is at least Lifecycle.State.CREATED.

//        lifecycleScope.launchWhenCreated {
//            viewModel.countryListFlow.collect { countryPresentationList ->
//                binding?.textLoadingData?.isVisible = false
//                countryList =
//                    countryPresentationList // Заполним массив для последующей обработки клика
//                showProgress()
//                countryPresentationList.forEach { countryPresentation ->
//                    addMarkersOnMap(countryPresentation)
//                }
//                hideProgress()
//
//                if (countryPresentationList == emptyList<CountryPresentation>()) { // Мы переходим на эту страницу только если БД не пуста. Если массив пустой - что-то пошло не так
//                    showProgress()
//                    Toast.makeText(
//                        context,
//                        "Something went wrong. Waiting for database response.",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            }
//        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {

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

    }

// Intent
//    inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
//        SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
//        else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
//    }

// Bundle
    private inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
        SDK_INT >= 33 -> getParcelable(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelable(key) as? T
    }

// Bundle & Intent ArrayList
//    inline fun <reified T : Parcelable> Bundle.parcelableArrayList(key: String): ArrayList<T>? = when {
//        SDK_INT >= 33 -> getParcelableArrayList(key, T::class.java)
//        else -> @Suppress("DEPRECATION") getParcelableArrayList(key)
//    }
//    inline fun <reified T : Parcelable> Intent.parcelableArrayList(key: String): ArrayList<T>? = when {
//        SDK_INT >= 33 -> getParcelableArrayListExtra(key, T::class.java)
//        else -> @Suppress("DEPRECATION") getParcelableArrayListExtra(key)
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

// Comparing image views:
//                    val starId = activity?.findViewById(R.id.image_star) as ImageView
//                    val bmapStar = (starId.drawable as BitmapDrawable).bitmap
//                    val myDrawableForComparing = resources.getDrawable(R.drawable.star)
//                    val bmapDrawableForComparing = (myDrawableForComparing as BitmapDrawable).bitmap
//
//                    val isSame = bmapStar.sameAs(bmapDrawableForComparing)

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

                    Toast.makeText(
                        context,
                        "Asking server for the radio station list...",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

//    // TOOLBAR
//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        super.onCreateOptionsMenu(menu, inflater)
//        inflater.inflate(R.menu.home_toolbar_menu, menu)
//    }
//
//    // TOOLBAR - обработка клика
//    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
//        R.id.log_out -> {
//            showLogoutDialog()
//            Log.d(TAG, "showLogoutDialog() was called")
//            true
//        }
//
//        else -> {
//            // If we got here, the user's action was not recognized.
//            // Invoke the superclass to handle it.
//            Log.d(TAG, "else result")
//            super.onOptionsItemSelected(item)
//        }
//    }

    // TOOLBAR - Описываем метод из интерфейса ILogOutListener для выхода из аккаунта приложения
    override fun onLogOut() {
        viewModel.logout()
        activity?.finish()
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

}