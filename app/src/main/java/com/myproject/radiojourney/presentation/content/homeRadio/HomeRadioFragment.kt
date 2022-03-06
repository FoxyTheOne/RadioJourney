package com.myproject.radiojourney.presentation.content.homeRadio

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.myproject.radiojourney.IAppSettings
import com.myproject.radiojourney.R
import com.myproject.radiojourney.model.local.Place
import com.myproject.radiojourney.presentation.content.base.BaseContentFragmentAbstract
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Главная страница.
 * Содержит карту с метками, описание выбранной радиостанции и кнопки "добавить в избранное", "перейти в мой список".
 *
 * С View binding не работает обработка клика Toolbar?
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

    @Inject
    lateinit var appSettings: IAppSettings

    private val viewModel by viewModels<HomeRadioViewModel>()
    private lateinit var frameLayout: FrameLayout
    private lateinit var progressCircular: ProgressBar

    // Переменная для нашего FusedLocationProviderClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // GOOGLE MAPS -> 2.1. Объявляем переменную, в соответствии с инструкцией от гугла
    private lateinit var mMap: GoogleMap

    // GOOGLE MAPS -> 3.6. Объявляем переменную для маркера
    private var marker: Marker? = null
    private var customMarkerYouAreHere: Bitmap? = null
    private var customMarkerRadio: Bitmap? = null

    // Кнопки на карте
    private lateinit var buttonZoomPlus: Button
    private lateinit var buttonZoomMinus: Button
    private lateinit var buttonYouAreHere: Button
    // ADD MARKERS TO MAP -> 1. Для примера, сейчас. Потом подгружать список по запросу
    private val places: List<Place> = listOf(
        Place(name = "Minsk", latLng = LatLng(53.90580039557321, 27.562806971874416))
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // TOOLBAR
        setHasOptionsMenu(true)
        val view = inflater.inflate(R.layout.layout_home_radio, container, false)
        // TOOLBAR - где будет находиться в нашем layout
        appSettings.setToolbar(view?.findViewById(R.id.home_toolbar))
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // LOCATION -> 1.4. Получим наш FusedLocationProviderClient. Именно он имеет в себе методы, с помощью которых мы можем определить локацию
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())
        // Переменные для отображения прогресса
        frameLayout = view.findViewById(R.id.frameLayout)
        progressCircular = view.findViewById(R.id.progressCircular)
        // Кнопки на карте
        buttonZoomPlus = view.findViewById(R.id.button_zoomPlus)
        buttonZoomMinus = view.findViewById(R.id.button_zoomMinus)
        buttonYouAreHere = view.findViewById(R.id.button_youAreHere)

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
            addMarkers(googleMap)
            // Set custom info window adapter
            googleMap.setInfoWindowAdapter(MarkerInfoWindowAdapter(requireContext()))
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
                R.drawable.radio_icon
            ) as BitmapDrawable).bitmap,
            100,
            100,
            false
        )

        // LOCATION -> 1.5. Создадим метод для получения Current location либо Last location
        getCurrentOrLastLocation()
        // Получить локацию нужно разово, при открытии фрагмента. Обновлять не нужно.

        initListeners()
        subscribeOnLiveData()
    }

    private fun initListeners() {
        // Кнопки на карте
        buttonZoomPlus.setOnClickListener {}
        buttonZoomMinus.setOnClickListener {}
        buttonYouAreHere.setOnClickListener {
            getCurrentOrLastLocation()
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
    }

    private fun showProgress() {
        frameLayout.isVisible = true
        progressCircular.isVisible = true
    }

    private fun hideProgress() {
        frameLayout.isVisible = false
        progressCircular.isVisible = false
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
    private fun addMarkers(googleMap: GoogleMap) {
        places.forEach { place ->
            customMarkerRadio?.let { customBitmapMarker ->
                val marker = googleMap.addMarker(
                    MarkerOptions()
                        .title(place.name)
                        .snippet("Открыть список радиостанций")
                        .position(place.latLng)
                        .icon(BitmapDescriptorFactory.fromBitmap(customBitmapMarker))
                )
            }

            // Set place as the tag on the marker object so it can be referenced within
            // MarkerInfoWindowAdapter
            marker?.tag = place
        }
    }

    // GOOGLE MAPS -> 2.3. Сюда прилетит карта, когда она будет готова к работе.
    // Когда она сюда залетит, мы сможем к ней обратиться и сказать, что она будет равна нашей переменной класса
    // !!! This method passes a GoogleMap instance to you, which you can then use to perform various operations on the map.
    override fun onMapReady(map: GoogleMap) {
        this.mMap = map
        // Далее по документации здесь делают некоторые действия, однако мы сделаем их в отдельном методе
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
        viewModel.logout()
        this.findNavController().navigate(R.id.action_homeRadioFragment_to_auth_nav_graph)
    }

}