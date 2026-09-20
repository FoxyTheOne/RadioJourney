package com.myproject.radiojourney.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.myproject.radiojourney.R
import com.myproject.radiojourney.databinding.ActivityMainBinding
import com.myproject.radiojourney.presentation.model.RadioStationPresentation
import com.myproject.radiojourney.other.Constants
import com.myproject.radiojourney.presentation.common.InfoDialog
import com.myproject.radiojourney.presentation.common.collectWhenStarted
import com.myproject.radiojourney.presentation.common.isInternetAvailable
import com.myproject.radiojourney.presentation.content.homeRadio.HomeRadioFragmentDirections
import com.myproject.radiojourney.presentation.content.radioStationList.adapter.SwipeRadioStationAdapter
import com.myproject.radiojourney.utils.extension.startStationIndex
import com.myproject.radiojourney.utils.exoplayer.PlaybackStateInfo
import dagger.hilt.android.AndroidEntryPoint

/**
 * This source code is free for studying purposes but you are not allowed to copy and use it in other applications (projects).
 *
 * Данный проект представляет собой приложение для прослушивания интернет радиостанций. Я использую API.radio-browser.info,
 * который предоставляет доступ к собранным интернет-радиостанциям со всего мира (https://www.radio-browser.info/).
 * Этот API доступен бесплатно. Автор разрешает его использовать в бесплатном и платном программном обеспечении без ограничений.
 *
 * На главной странице вы найдете google карту с маркерами, нажимая на которые можно увидеть количество доступных в этой стране
 * интернет радиостанций. Нажав на это сообщение, открывается список интернет радиостанций в выбранной стране (recycler view),
 * где можно выбрать интересующее радио.
 *
 * По клику на радио, пользователь возвращается на главный экран и может его прослушать, если это радио в данный момент работает.
 *
 * Перед входом в приложение запрашивается разрешение на доступ к местоположению.
 *
 * - В проекте используется архитектурный паттерн MVVM и подход Clean Architecture;
 * - Используется DI – Hilt, а также Navigation component и View Binding;
 * - Для хранения небольших пар ключ-значение (токен и тп.) я использую Shared preferences;
 * - Для сохранения локаций маркеров на карте, а также для хранения избранных радиостанций используется реляционная база данных Room.
 * При первом запуске нужно дождаться окончания кеширования, в дальнейшем данные берутся из подписки на локальную базу данных;
 * - Список стран для карты загружается в фоне с помощью WorkManager;
 * - Все запросы на сервер, либо в локальную БД из ViewModel я делаю через Coroutines;
 * - Для запроса на сервер используется Retrofit2.
 *
 * This project is an application for listening to Internet radio stations. I am using API.radio-browser.info
 * which allows you to access to collected internet radio stations from all over the world (https://www.radio-browser.info/).
 * This API is available for free. The author allows to use it in free and commercial software without restrictions.
 *
 * On the main page you will find a google map with markers, by clicking on which you can see the number of available
 * Internet radio stations in this country. A list of Internet radio stations in the selected country (recycler view)
 * can be opened by clicking on this message and then you can select the radio you are interested in.
 *
 * By clicking on the radio, the user returns to the main screen and can listen to it if this radio is currently working.
 *
 * Before entering the application, you are asked for your location permission.
 *
 * - The project uses the MVVM architectural pattern and the Clean Architecture concept;
 * - Hilt is used here, as well as Navigation component and View Binding;
 * - To store small key-value pairs (token for instance), I use Shared preferences;
 * - The Room database is used to store marker locations on the map, as well as to store favorite radio stations.
 * You need to wait until caching ends at the first start. Further the data is taken from the subscription to the local database;
 * - The country list for the map is loaded in the background with WorkManager;
 * - I make all requests to the server, or to the local database from the ViewModel, through Coroutines;
 * - For the request to the server, Retrofit2 is used.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: ActivityMainBinding? = null

    private val mainViewModel by viewModels<MainViewModel>() // Такую же view model мы зарегистрировали в homeFragment. Основная ViewModel, для общения с плейером в bottom bar

    // Variable for currently playing song
    private var curPlayingRadioStation: RadioStationPresentation? = null
    private var playbackState: PlaybackStateInfo? = null

    private var mOnPageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private val swipeRadioStationAdapter = SwipeRadioStationAdapter()

    private lateinit var infoDialog: InfoDialog
    private lateinit var navController: NavController

    // Действия, которые ждут, пока плейлист появится в ViewPager (см. whenPlaylistReady)
    private val pendingWhenPlaylistReady = mutableListOf<() -> Unit>()

    // Версия плейлиста, которая уже показана в ViewPager (см. onPlaylistChanged)
    private var shownPlaylistVersion = 0

    // Запрос на разрешение notification. Регистрируется полем класса - до создания Activity, как требует Activity Result API
    private val requestPermissionLauncherNotification =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(
                    this,
                    "We don't have permission to show notifications on your Android",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // VIEW BINDING -> 2. Инициализация
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view: View = binding!!.root
        setContentView(view)
        applySystemBarInsets(view)

        // Навигация. Условная навигация по рекомендации developer.android.com: стартовый экран выбирается до его создания.
        // Раньше первый экран открывался всегда, а уже в его onCreate выполнялся переход на карту (BaseAuthFragmentAbstract)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController
        val navGraph = navController.navInflater.inflate(R.navigation.app_navigation).apply {
            setStartDestination(
                if (mainViewModel.isLoggedIn()) R.id.homeRadioFragment else R.id.firstScreenLoadingFragment
            )
        }
        // При пересоздании Activity NavController сам восстановит открытые экраны поверх этого графа
        navController.setGraph(navGraph, null)

        binding?.imageStar?.setImageResource(R.drawable.ic_baseline_star_border_24_orange)

        mOnPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {

                // Тестово добавляю это сюда тоже, т.к. прогресс не всегда убирается - 2
                // Favorite star
                if (swipeRadioStationAdapter.radioStationList.isNotEmpty()) {
                    checkFavoriteStarIfCountryCodeIsRight(
                        mainViewModel.curPlayingSong.value?.mediaMetadata?.subtitle.toString(),
                        swipeRadioStationAdapter.radioStationList[0].countryCode
                    )
                }

                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // Если выбрать радиостанцию US (2000 Rock ...), а после неё первое Белорусское радио в списке (альфарадио) - вылетает IndexOutOfBoundsException, т.к. сначала ищет 300+ индекс в списке из 53х
                val swipeRadioStationList = swipeRadioStationAdapter.radioStationList
                val maxRadioStationListIndex = swipeRadioStationAdapter.radioStationList.size - 1
                if (swipeRadioStationList.isNotEmpty() && maxRadioStationListIndex >= position) {

                    try {
                        // Нам нужно вернуться в onPrepareFromMediaId, если мы выбрали песню из другого плейлиста и включить её. НО! Нам не нужно включать станцию сразу при включении программы
                        val isNotJustLaunched = mainViewModel.isNotJustLaunched.value

                        // We must check, if player is playing
                        // Добавляю "&& isNotJustLaunched == true" для того, чтобы туда не заходило при повторном запуске приложения (когда станция играет из уведомления и ты кликаешь на уведомление)
                        if (playbackState?.isPlaying == true && isNotJustLaunched) {
                            mainViewModel.playOrToggleSong(swipeRadioStationList[position])
                            Log.d(
                                TAG,
                                "PLAYLIST_UPDATE: 4.$TAG. Метод onPageSelected() -> Плейер проигрывает радиостанцию. Программа не только что запущена. Вызываем mainViewModel.playOrToggleSong(swipeRadioStationList[position])"
                            )
                        } else {
                            // При включении программы и загрузке контента так же попадаем сюда
                            curPlayingRadioStation = swipeRadioStationList[position]

                            // Первый запуск
                            if (!isNotJustLaunched) {
                                switchViewPagerToCurrentSong(
                                    swipeRadioStationList[position].stationuuid,
                                    swipeRadioStationList[position].countryCode
                                )
                                Log.d(
                                    TAG,
                                    "PLAYLIST_UPDATE: 4.$TAG. Метод onPageSelected() -> Плейер остановлен. Программа только что запущена. Вызываем switchViewPagerToCurrentSong()"
                                )
                            }

                            // Не первый запуск
                            if (isNotJustLaunched) {
                                // Здесь мы точно перешли из списка в HomeRadioFragment и хотим включить радио
                                mainViewModel.playOrToggleSong(
                                    swipeRadioStationList[position],
                                    true
                                )
                                Log.d(
                                    TAG,
                                    "PLAYLIST_UPDATE: 4.$TAG. Метод onPageSelected() -> Плейер остановлен. Программа не только что запущена. Вызываем mainViewModel.playOrToggleSong(swipeRadioStationList[position], true)"
                                )
                                // Если список пуст, значит это список избранного, который не заполнен. Но проверку на заполненность списка мы уже сделали
                            }
                        }
                    } catch (e: IndexOutOfBoundsException) {
                        Log.d(TAG, "CAUGHT IndexOutOfBoundsException!")
                        e.printStackTrace()
                    }

                }

                // Тестово добавляю это сюда тоже, т.к. прогресс не всегда убирается
                // Favorite star
                if (swipeRadioStationAdapter.radioStationList.isNotEmpty()) {
                    checkFavoriteStarIfCountryCodeIsRight(
                        mainViewModel.curPlayingSong.value?.mediaMetadata?.subtitle.toString(),
                        swipeRadioStationAdapter.radioStationList[0].countryCode
                    )
                }

            }
        }

        infoDialog = InfoDialog(this, R.layout.layout_please_wait_dialog, R.id.title_pleaseWait, R.id.text_pleaseWait)

        // Запрос на разрешение notification (уведомление плеера).
        // Разрешение FOREGROUND_SERVICE раньше тоже запрашивалось здесь, но оно выдаётся при установке и в запросе не нуждается
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Если нет разрешения - вызываем requestPermissionLauncher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncherNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // COUNTRY LIST MARKERS ON MAP -> 1. Список стран загружается в MainViewModel (WorkManager, CountryCacheWorker) - один раз за запуск приложения

        initListeners()
        subscribeToObservers()

    }

    private fun initListeners() {
        // To detect if it is swiped
        mOnPageChangeCallback?.let {
            binding?.vpSong?.registerOnPageChangeCallback(it)
        }

        // Click listener (on play image)
        binding?.ivPlayPause?.setOnClickListener {

            // Проверяем подключение к интернету
            if (!isInternetAvailable()) {
                // Диалоговое окно при отсутствии интернета
                infoDialog.show(
                    R.string.dialogInternetTrouble_title,
                    R.string.dialogInternetTrouble_text3
                )
            }

            curPlayingRadioStation?.let {
                mainViewModel.playOrToggleSong(it, true) // true, because now we want to autoplay
                mainViewModel.notJustLaunchedEnableAutoplay()
            }
        }

        // При нажатии на плейер, открывается список радиостанций в текущем плейлисте
        swipeRadioStationAdapter.setItemClickListener {
            val direction =
                HomeRadioFragmentDirections.actionHomeRadioFragmentToCurrentPlaylistFragment()
            if (navController.currentDestination?.id == R.id.homeRadioFragment) {
                navController.navigate(direction)
            }
        }

        // Let's add a listener to our NavController to hide BottomBar when we are on the first page, where we are cashing
        navController
            .addOnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    R.id.firstScreenLoadingFragment -> hideBottomBar()
                    R.id.homeRadioFragment -> showBottomBar()
                    else -> showBottomBar()
                }
            }

        binding?.imageStar?.setOnClickListener {
            val radioStationList = swipeRadioStationAdapter.radioStationList
            val radioStationPosition = binding?.vpSong?.currentItem ?: return@setOnClickListener
            var currentRadioStation: RadioStationPresentation? = null

            if (radioStationList.isNotEmpty()) {
                currentRadioStation = radioStationList[radioStationPosition]
            }

            currentRadioStation?.let {
                mainViewModel.checkIsStationInFavouritesAndChangeTheStar(it)
            }
        }
    }

    private fun switchViewPagerToCurrentSong(mediaId: String, countryCode: String) {
        val curPlayingMediaId = mainViewModel.curPlayingSong.value?.mediaId
        if (mediaId != curPlayingMediaId) return

        Log.d(
            TAG,
            "PLAYLIST_UPDATE: 4.$TAG, switchViewPagerToCurrentSong() вызван"
        )
        var radioStationNeedToFind: RadioStationPresentation? = null

        if (swipeRadioStationAdapter.radioStationList.isNotEmpty()) {
            swipeRadioStationAdapter.radioStationList.forEach {
                if (it.stationuuid == mediaId) {
                    radioStationNeedToFind = it
                }
            }
        }

        radioStationNeedToFind?.let {
            val newItemIndex =
                swipeRadioStationAdapter.radioStationList.indexOf(radioStationNeedToFind) // looking for the index of that song

            // That function will return -1 if the song doesn't exist, so we must check:
            if (newItemIndex != -1) {

                binding?.vpSong?.doOnLayout {
                    Log.d(
                        TAG,
                        "PLAYLIST_UPDATE: 4.$TAG, switchViewPagerToCurrentSong(). Обновляем наш vpSong, newItemIndex = $newItemIndex"
                    )
                    binding?.vpSong?.setCurrentItem(newItemIndex, false)
                }

                curPlayingRadioStation =
                    radioStationNeedToFind // we also update our curPlayingRadioStation
            }

        }
    }

    private fun subscribeToObservers() {
        // Полоса загрузки плейлиста: 0..70% - обработка станций (дальше 85% и 95% ставятся ниже, когда список показан и станция выбрана).
        // Раньше прогресс приходил бродкастом из MusicService
        collectWhenStarted(mainViewModel.playlistDownloadProgress) { percent ->
            binding?.progressBarHorizontalDp?.progress = 70 * percent / 100
        }

        // Не удалось скачать плейлист: сервер недоступен (раньше - LocalBroadcastManager из MusicService)
        collectWhenStarted(mainViewModel.serverIsDown) {
            infoDialog.show(R.string.dialogInternetTrouble_title4, R.string.dialogInternetTrouble_text4)
            mainViewModel.hideProgressAndSetClickable(true)
        }

        // Плейлист плеера: заполняем ViewPager2 и выбираем станцию
        collectWhenStarted(mainViewModel.playlist) { playlist ->
            if (playlist != null) onPlaylistChanged(playlist) // null - плейлист ещё не загружен
        }

        // Every time we have new info about currently playing song (when the song switches)
        collectWhenStarted(mainViewModel.curPlayingSong) { curPlayingSong ->
            if (curPlayingSong == null) return@collectWhenStarted

            whenPlaylistReady {
                // Полоса прогресса: станция выбрана
                binding?.progressBarHorizontalDp?.progress = 95

                val countryCode = curPlayingSong.mediaMetadata.subtitle.toString()
                switchViewPagerToCurrentSong(curPlayingSong.mediaId, countryCode)

                // Favorite star. Если список радиостанций не пуст и код страны одинаковый в vpSong и плейере, скроется прогресс
                if (swipeRadioStationAdapter.radioStationList.isNotEmpty()) {
                    checkFavoriteStarIfCountryCodeIsRight(countryCode, swipeRadioStationAdapter.radioStationList[0].countryCode)
                }
            }
        }

        // Иногда сбивается и в уведомлении показывает правильную станцию, а в плейере - нет. Добавляю страховку
        collectWhenStarted(mainViewModel.switchViewPagerOnceAgain) { radioStation ->
            whenPlaylistReady {
                switchViewPagerToCurrentSong(radioStation.stationuuid, radioStation.countryCode)
            }
        }

        // Will be called everytime the playback changes (pause the player, play a song etc.) -> change our image
        collectWhenStarted(mainViewModel.playbackState) { state ->
            playbackState = state
            binding?.ivPlayPause?.setImageResource(
                if (state?.isPlaying == true) R.drawable.ic_pause_orange else R.drawable.ic_play_arrow_orange
            )

            // "Connecting to radio station" прячем, как только плейер после её показа начал играть или сообщил об ошибке
            // (ошибка - это тот же момент, когда сервис показывает toast). Ждать смены метаданных нельзя:
            // при повторном выборе той же станции (например, после ошибки) метаданные не меняются, и полоса висела до таймаута
            val connectingShownAt = mainViewModel.connectingProgressShownAt
            if (connectingShownAt != null && state != null && state.updateTime >= connectingShownAt &&
                (state.isActuallyPlaying || state.hasError)
            ) {
                mainViewModel.hideProgressAndSetClickable()
            }
        }

        // Ошибки подключения к сервису, сети и прочие: сообщение, убираем прогресс и делаем кнопки снова кликабельными.
        // Раньше - три одинаковых наблюдателя LiveData с классом Event
        collectWhenStarted(mainViewModel.errorMessages) { message ->
            Log.d(TAG, "Error: $message")
            binding?.let { nonNullBinding ->
                Snackbar.make(nonNullBinding.rootLayout.rootView, message, Snackbar.LENGTH_LONG).show()
            }
            mainViewModel.hideProgressAndSetClickable()
        }

        // Favourites: звезда нажата где угодно (в плейере, списке избранного).
        // Звезду в плейере меняем, только если показана именно эта станция. Признак в плейлисте обновляет MainViewModel
        collectWhenStarted(mainViewModel.favouriteChanges) { change ->
            val shownStation = swipeRadioStationAdapter.radioStationList.getOrNull(binding?.vpSong?.currentItem ?: -1)
            if (shownStation?.stationuuid == change.station.stationuuid) {
                binding?.imageStar?.setImageResource(
                    if (change.isFavourite) R.drawable.ic_baseline_star_24_orange else R.drawable.ic_baseline_star_border_24_orange
                )
            }
        }

        // Полоса загрузки поверх плейера. Раньше - три LiveData (setNonClickableDp, setNonClickableCRSt, setClickable)
        collectWhenStarted(mainViewModel.loadingState) { loadingState ->
            val isLoading = loadingState != MainViewModel.LoadingState.NONE
            swipeRadioStationAdapter.isClickableRecyclerView = !isLoading
            binding?.frameLayoutDp?.isVisible = isLoading
            binding?.text1Dp?.isVisible = isLoading
            binding?.progressBarHorizontalDp?.isVisible = isLoading
            when (loadingState) {
                MainViewModel.LoadingState.DOWNLOADING_PLAYLIST -> binding?.text1Dp?.text = getString(R.string.downloading_playlist)
                MainViewModel.LoadingState.CONNECTING_STATION -> binding?.text1Dp?.text = getString(R.string.downloading_radio_station)
                MainViewModel.LoadingState.NONE -> Unit
            }
        }
    }

    private fun onPlaylistChanged(playlist: MainViewModel.Playlist) {
        val radioStations = playlist.stations

        // Та же версия плейлиста - у станции изменился только признак избранного (MainViewModel.notifyFavouriteChanged),
        // или экран снова стал видимым. Обновляем список в адаптере, но станцию не переключаем
        if (playlist.version == shownPlaylistVersion) {
            swipeRadioStationAdapter.radioStationList = radioStations
            return
        }
        shownPlaylistVersion = playlist.version

        if (radioStations.isNotEmpty()) {
            // Всё, что ниже, выполняем только после того, как адаптер применит новый список.
            // Раньше onPageSelected(0) вызывался сразу после присваивания radioStationList, но AsyncListDiffer
            // ещё возвращал СТАРЫЙ список -> playOrToggleSong() получал первую станцию старого плейлиста,
            // считал её "той же самой" и новую станцию не включал
            swipeRadioStationAdapter.submitRadioStationList(radioStations) {
                binding?.vpSong?.adapter = swipeRadioStationAdapter

                // !!! Новый плейлист начинаем не с первой по алфавиту станции (во многих странах это одни и те же
                // станции вроде ".Quran" или "# TOP 100 ..."), а с самой популярной.
                // Но если в плеере уже станция из этого же плейлиста (например, Activity пересоздана при смене темы),
                // остаёмся на ней. Раньше ViewPager в этом случае уходил на самую популярную станцию
                // и переключал на неё плеер, если радио было на паузе
                val curPlayingSong = mainViewModel.curPlayingSong.value
                val curPlayingIndex = radioStations.indexOfFirst {
                    it.stationuuid == curPlayingSong?.mediaId &&
                            it.countryCode == curPlayingSong.mediaMetadata.subtitle.toString()
                }
                val startPosition = if (curPlayingIndex != -1) curPlayingIndex else radioStations.startStationIndex()
                val vpSong = binding?.vpSong
                if (vpSong != null && vpSong.currentItem != startPosition) {
                    vpSong.setCurrentItem(startPosition, false) // ViewPager сам вызовет onPageSelected(startPosition)
                } else {
                    // Почему-то этот метод изредка не вызывается, хотя должен. На всякий случай дублирую вызов здесь
                    mOnPageChangeCallback?.onPageSelected(startPosition)
                }

                // Полоса прогресса: список показан
                binding?.progressBarHorizontalDp?.progress = 85

                // Если не включать плейер, а просто листать от списка к списку, onPageScrolled перестаёт вызываться на четвёртый раз
                // и звезда перестаёт меняться (избранное/не избранное). Поэтому на всякий случай вызываю его дополнительно
                mOnPageChangeCallback?.onPageScrolled(0, 0.0f, 0)

                // Возвращаем ViewPager на станцию, которая сейчас в плейере. Берём её из метаданных, а не из curPlayingRadioStation:
                // onPageSelected выше уже записал туда станцию списка
                if (curPlayingSong != null) {
                    switchViewPagerToCurrentSong(curPlayingSong.mediaId, curPlayingSong.mediaMetadata.subtitle.toString())
                }
            }
        }

        mainViewModel.stateInitialized()
        runPendingWhenPlaylistReady()
    }

    // Начиная с targetSdk 35, на Android 15+ окно всегда рисуется под строкой состояния и панелью навигации (edge-to-edge),
    // а цвет строки состояния из темы (android:statusBarColor) не применяется: заголовок заходил под часы,
    // а плеер внизу - под полоску жестов. Добавляем отступы на размер системных панелей и сами рисуем
    // оранжевый фон под строкой состояния, как было раньше. На старых версиях Android отступы равны 0
    private fun applySystemBarInsets(rootView: View) {
        val statusBarColor = ContextCompat.getColor(this, R.color.orange)
        val backgroundColor = ContextCompat.getColor(this, R.color.background)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)

            // Фон: сверху полоса цвета строки состояния, остальное - тёмный фон приложения
            v.background = LayerDrawable(arrayOf(ColorDrawable(backgroundColor), ColorDrawable(statusBarColor))).apply {
                setLayerGravity(1, Gravity.TOP or Gravity.FILL_HORIZONTAL)
                setLayerHeight(1, bars.top)
            }
            WindowInsetsCompat.CONSUMED
        }

        // Светлые значки на оранжевой строке состояния и тёмной панели навигации
        WindowCompat.getInsetsController(window, rootView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    private fun checkFavoriteStarIfCountryCodeIsRight(
        countryCodeInExoplayer: String,
        countryCodeInVp: String
    ) {
        Log.d(
            TAG,
            "1. FAV_STAR: Radio station list is not empty = ${swipeRadioStationAdapter.radioStationList.isNotEmpty()}. Countrycode in exoplayer = $countryCodeInExoplayer. Countrycode in vp = $countryCodeInVp"
        )

        if (swipeRadioStationAdapter.radioStationList.isNotEmpty()) {

            if ((countryCodeInExoplayer.endsWith("_FAV", true)
                        && countryCodeInVp.endsWith("_FAV", true))
                || countryCodeInExoplayer == countryCodeInVp
            ) {

                Log.d(
                    TAG,
                    "2. FAV_STAR: Entered to check favorite star and hide progress"
                )

                val currentRadioStationPosition = binding?.vpSong?.currentItem

                currentRadioStationPosition?.let { position ->
                    // java.lang.IndexOutOfBoundsException: Index: 14, Size: 4
                    if (position < swipeRadioStationAdapter.radioStationList.size) {
                        Log.d(
                            TAG,
                            "2. FAV_STAR: Checking favorite star and hiding progress"
                        )

                        val isStationInFavourite =
                            swipeRadioStationAdapter.radioStationList[position].isStationInFavourite
                        Log.d(
                            TAG, "2. FAV_STAR: isStationInFavourite = $isStationInFavourite"
                        )
                        if (isStationInFavourite) {
                            binding?.imageStar?.setImageResource(R.drawable.ic_baseline_star_24_orange)
                        } else {
                            binding?.imageStar?.setImageResource(R.drawable.ic_baseline_star_border_24_orange)
                        }

                        // Убираем прогресс и делаем кнопки снова кликабельными
                        mainViewModel.hideProgressAndSetClickable()
                        Log.d(
                            TAG,
                            "BROADCAST: Прячем прогресс. Вызываем метод hideProgressAndSetClickable()"
                        )
                    }
                }

            } else {
                Log.d(
                    TAG,
                    "2. FAV_STAR: DID NOT enter to check favorite star and hide progress"
                )
            }

        }

    }

    private fun hideBottomBar() {
        binding?.imageStar?.isVisible = false
        binding?.vpSong?.isVisible = false
        binding?.ivPlayPause?.isVisible = false
    }

    private fun showBottomBar() {
        binding?.imageStar?.isVisible = true
        binding?.vpSong?.isVisible = true
        binding?.ivPlayPause?.isVisible = true
    }

    // Выполнить действие, когда плейлист показан в ViewPager (сразу, если уже показан).
    // Ожидающие действия хранятся в Activity, а не в MainViewModel: они ссылаются на Activity и должны исчезать вместе с ней
    private fun whenPlaylistReady(action: () -> Unit) {
        if (mainViewModel.isPlaylistReady) action() else pendingWhenPlaylistReady += action
    }

    private fun runPendingWhenPlaylistReady() {
        val actions = pendingWhenPlaylistReady.toList()
        pendingWhenPlaylistReady.clear()
        actions.forEach { it() }
    }

    override fun onDestroy() {
        mOnPageChangeCallback?.let {
            binding?.vpSong?.unregisterOnPageChangeCallback(it)
        }
        pendingWhenPlaylistReady.clear()
        infoDialog.dismiss() // Открытый диалог закрываем вместе с Activity, иначе WindowLeaked

        binding = null // VIEW BINDING -> 3. onDestroyView()

        super.onDestroy()
    }
}