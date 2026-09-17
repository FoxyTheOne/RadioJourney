package com.myproject.radiojourney.presentation

import android.Manifest
import android.accounts.AccountsException
import android.app.ActivityManager
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.myproject.radiojourney.IAppSettings
import com.myproject.radiojourney.R
import com.myproject.radiojourney.databinding.ActivityMainBinding
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import com.myproject.radiojourney.other.Constants
import com.myproject.radiojourney.other.Constants.AUDIO_CONNECTING
import com.myproject.radiojourney.other.Constants.AUDIO_PLAYING
import com.myproject.radiojourney.other.Constants.AUDIO_STOPPED
import com.myproject.radiojourney.other.Constants.FILTER_FOR_BROADCAST_MA_SERVER
import com.myproject.radiojourney.other.Constants.KEY_BROADCAST_SERVER_IS_DOWN
import com.myproject.radiojourney.other.Status.ERROR
import com.myproject.radiojourney.other.Status.LOADING
import com.myproject.radiojourney.other.Status.SUCCESS
import com.myproject.radiojourney.presentation.content.homeRadio.HomeRadioFragmentDirections
import com.myproject.radiojourney.presentation.content.radioStationList.adapter.SwipeRadioStationAdapter
import com.myproject.radiojourney.utils.extension.startStationIndex
import com.myproject.radiojourney.utils.exoplayer.PlaybackStateInfo
import com.myproject.radiojourney.utils.service.ProgressForegroundService
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException

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
 * - Для отображения прогресса кеширования в уведомлении используется Foreground service;
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
 * - I use Foreground service to display caching progress in notification;
 * - I make all requests to the server, or to the local database from the ViewModel, through Coroutines;
 * - For the request to the server, Retrofit2 is used.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), IAppSettings {
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

    private lateinit var dialogPleaseWait: Dialog
    private var isInternetAvailable = false

    // Действия, которые ждут, пока плейлист появится в ViewPager (см. whenPlaylistReady)
    private val pendingWhenPlaylistReady = mutableListOf<() -> Unit>()

//    // 1. PROGRESS Текущий актуальный ID загрузки
//    private var currentPlaylistId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        setContentView(R.layout.activity_main) <- заменяем на view binding:
        // VIEW BINDING -> 2. Инициализация
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view: View = binding!!.root
        setContentView(view)
        applySystemBarInsets(view)

        binding?.imageStar?.setImageResource(R.drawable.ic_baseline_star_border_24_orange)

        mOnPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
//                if (swipeRadioStationAdapter.radioStationList.isNotEmpty()) {
//
//                    if (swipeRadioStationAdapter.radioStationList[position].isStationInFavourite) {
//                        binding?.imageStar?.setImageResource(R.drawable.ic_baseline_star_24_orange)
//                    } else {
//                        binding?.imageStar?.setImageResource(R.drawable.ic_baseline_star_border_24_orange)
//                    }
//
////                    // Убираем прогресс и делаем кнопки снова кликабельными
////                    mainViewModel.hideProgressAndSetClickable()
//
//                }

                // Тестово добавляю это сюда тоже, т.к. прогресс не всегда убирается - 2
                // Favorite star
                if (swipeRadioStationAdapter.radioStationList.isNotEmpty()) {
                    checkFavoriteStarIfCountryCodeIsRight(
                        mainViewModel.curPlayingSongLiveData.value?.mediaMetadata?.subtitle.toString(),
                        swipeRadioStationAdapter.radioStationList[0].countryCode
                    )
                }

                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }

            // function, that is called when the viewpager is swiped - onPageSelected()
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

//                mainViewModel.synchronizedCheckThePosition(
//                    position,
//                    mainViewModel.mediaItemsListLiveData.value?.data,
//                    swipeRadioStationAdapter.radioStationList
//                )

                // Если выбрать радиостанцию US (2000 Rock ...), а после неё первое Белорусское радио в списке (альфарадио) - вылетает IndexOutOfBoundsException, т.к. сначала ищет 300+ индекс в списке из 53х
                val swipeRadioStationList = swipeRadioStationAdapter.radioStationList
                val maxRadioStationListIndex = swipeRadioStationAdapter.radioStationList.size - 1
                if (swipeRadioStationList.isNotEmpty() && maxRadioStationListIndex >= position) {

                    try {
                        // Нам нужно вернуться в onPrepareFromMediaId, если мы выбрали песню из другого плейлиста и включить её. НО! Нам не нужно включать станцию сразу при включении программы
                        val isNotJustLaunched = mainViewModel.isNotJustLaunchedLiveData.value

                        // We must check, if player is playing
                        // Добавляю "&& isNotJustLaunched == true" для того, чтобы туда не заходило при повторном запуске приложения (когда станция играет из уведомления и ты кликаешь на уведомление)
                        if (playbackState?.isPlaying == true && isNotJustLaunched == true) {
                            mainViewModel.playOrToggleSong(swipeRadioStationList[position])
                            Log.d(
                                TAG,
                                "PLAYLIST_UPDATE: 4.$TAG. Метод onPageSelected() -> Плейер проигрывает радиостанцию. Программа не только что запущена. Вызываем mainViewModel.playOrToggleSong(swipeRadioStationList[position])"
                            )
                        } else {
                            // При включении программы и загрузке контента так же попадаем сюда
                            curPlayingRadioStation = swipeRadioStationList[position]

                            // Первый запуск
                            if (isNotJustLaunched == null || !isNotJustLaunched) {
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
                            isNotJustLaunched?.let {
                                if (it) {
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
                        }
                    } catch (e: IndexOutOfBoundsException) {
                        Log.d(TAG, "CAUGHT IndexOutOfBoundsException!")
                        e.printStackTrace()
                    } catch (e1: AccountsException) {
                        // AccountsException -> Known direct subclasses: AuthenticatorException, NetworkErrorException, OperationCanceledException
                        e1.printStackTrace()
                        mainViewModel.dialogInternetTroubleCall()
                    } catch (e2: IOException) {
                        Log.d(TAG, "An unknown error occurred in onPageSelected")
                        e2.printStackTrace()
                        mainViewModel.errorMessagePost("An unknown error occurred")
                    }

                }

                // Тестово добавляю это сюда тоже, т.к. прогресс не всегда убирается
                // Favorite star
                if (swipeRadioStationAdapter.radioStationList.isNotEmpty()) {
                    checkFavoriteStarIfCountryCodeIsRight(
                        mainViewModel.curPlayingSongLiveData.value?.mediaMetadata?.subtitle.toString(),
                        swipeRadioStationAdapter.radioStationList[0].countryCode
                    )
                }

            }
        }

        // Настройки диалогового окна
        dialogPleaseWait = Dialog(this)
        // Передайте ссылку на разметку
        dialogPleaseWait.setContentView(R.layout.layout_please_wait_dialog)

        // Запрос на разрешение Foreground
        val requestPermissionLauncherForeground =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (!isGranted) {
                    Toast.makeText(
                        this,
                        "We don't have permission to start foreground service",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Если нет разрешения - вызываем requestPermissionLauncher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                requestPermissionLauncherForeground.launch(Manifest.permission.FOREGROUND_SERVICE)
            }
        }

        // Запрос на разрешение notification
        val requestPermissionLauncherNotification =
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

        // COUNTRY LIST MARKERS ON MAP -> 1. Получаем список кодов стран, преобразуем в локальные модели, сохраняем в Room.
        // Делается 1 раз, при запуске приложения и по окончанию stopSelf()
        this.startService(
            Intent(
                this,
                ProgressForegroundService::class.java
            )
        )

        initListeners()
        subscribeToObservers()

//        // 1.Broadcast для отображения уведомления (2,3 - в MusicService)
//        val intentMS =
//            Intent(FILTER_FOR_BROADCAST_MS) // FILTER is a string to identify this intent
//        intentMS.apply {
//            Log.d(TAG, "Отправляем ключ KEY_BROADCAST_ACTIVITY, для отображения уведомления")
//            putExtra(KEY_BROADCAST_ACTIVITY, 50)
//            sendBroadcast(this)
//        }

    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            receiverServerIsDown,
            IntentFilter(FILTER_FOR_BROADCAST_MA_SERVER)
        )
        Log.d(
            TAG,
            "LocalBroadcastManager.BROADCAST: Регистрируемся в onStart() - когда получаем нулевой список, обычный бродкаст не работает (зависает полоса прогресса)"
        )
    }

    // 3.Broadcast для горизонтальной полосы прогресса в activity (1 - в ???)
    override fun onResume() {
        super.onResume()
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(Constants.FILTER_FOR_BROADCAST_MA),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
//        registerReceiver(receiver, IntentFilter(Constants.FILTER_FOR_BROADCAST_MA))
        Log.d(TAG, "BROADCAST: Регистрируемся в onResume()")
    }

    // 3.Broadcast - регистрируем в onResume и отписываемся в onPause
    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
        Log.d(TAG, "BROADCAST: Отписываемся в onPause()")
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverServerIsDown)
        Log.d(TAG, "LocalBroadcastManager.BROADCAST: Отписываемся в onStop()")
    }

    private fun initListeners() {
        // To detect if it is swiped
        mOnPageChangeCallback?.let {
            binding?.vpSong?.registerOnPageChangeCallback(it)
        }

        // Click listener (on play image)
        binding?.ivPlayPause?.setOnClickListener {

            // Проверяем подключение к интернету
            isInternetAvailable = mainViewModel.isInternetAvailable(this)
            if (!isInternetAvailable) {
                // Диалоговое окно при отсутствии интернета
                showCustomDialog(
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
            if (this.findNavController(R.id.navHostFragment).currentDestination?.id == R.id.homeRadioFragment) {
                this.findNavController(R.id.navHostFragment).navigate(direction)
            }
        }

        // Let's add a listener to our NavController to hide BottomBar when we are on the first page, where we are cashing
        this.findNavController(R.id.navHostFragment)
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
        val curPlayingMediaId = mainViewModel.curPlayingSongLiveData.value?.mediaId
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
//                binding?.vpSong?.currentItem =
//                    newItemIndex // currentItem - is the index of the song, that is displayed. We change it to a new one

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

//            // Убираем прогресс и делаем кнопки снова кликабельными
//            mainViewModel.hideProgressAndSetClickable()
        }
    }

    private fun subscribeToObservers() {
        // LIVEDATA: to fill our widget.ViewPager2 with correct items, display right ones WHEN WE LAUNCH OUR APP
        mainViewModel.mediaItemsListLiveData.observe(this) {
            it?.let { result ->
                Log.d(
                    "UI_DEBUG",
                    "PLAYER_DEBUG: MediaItems update. Status: ${result.status}, data size: ${result.data?.size}"
                )
                when (result.status) {
                    SUCCESS -> {

                        result.data?.let { radioStations ->
                            Log.d(
                                "UI_DEBUG",
                                "PLAYER_DEBUG: Updating ViewPager with ${radioStations.size} stations"
                            )

                            if (radioStations.isNotEmpty()) {

//                                <!-- 002 claude
//                                swipeRadioStationAdapter.radioStationList = radioStations

                                // Всё, что ниже, выполняем только после того, как адаптер применит новый список.
                                // Раньше onPageSelected(0) вызывался сразу после присваивания radioStationList, но AsyncListDiffer
                                // ещё возвращал СТАРЫЙ список -> playOrToggleSong() получал первую станцию старого плейлиста,
                                // считал её "той же самой" и новую станцию не включал. Срабатывало, только если текущая станция была
                                // не первой: тогда ViewPager сам вызывал onPageSelected(0) позже, когда новый список уже применён
                                swipeRadioStationAdapter.submitRadioStationList(radioStations) {
//                                002 claude -->

                                    // if we had an individual image
//                            if(radioStations.isNotEmpty()) {
//                                glide.load((curPlayingSong ?: radioStations[0]).imageUrl).into(ivCurSongImage)
//                            }

                                    // Попробуем назначить адаптер после обновления списка радиостанций
                                    binding?.vpSong?.adapter = swipeRadioStationAdapter

//                                    <!-- 006 claude
//                                    mOnPageChangeCallback?.onPageSelected(0)
//                                    // Почему-то этот метод изредка не вызывается, хотя должен. На всякий случай дублирую вызов здесь

                                    // !!! Новый плейлист начинаем не с первой по алфавиту станции (во многих странах это одни и те же
                                    // станции вроде ".Quran" или "# TOP 100 ..."), а с самой популярной
                                    val startPosition = radioStations.startStationIndex()
                                    val vpSong = binding?.vpSong
                                    if (vpSong != null && vpSong.currentItem != startPosition) {
                                        vpSong.setCurrentItem(
                                            startPosition,
                                            false
                                        ) // ViewPager сам вызовет onPageSelected(startPosition)
                                    } else {
                                        mOnPageChangeCallback?.onPageSelected(startPosition)
                                        // Почему-то этот метод изредка не вызывается, хотя должен. На всякий случай дублирую вызов здесь
                                    }
                                    // 006 claude -->

//                                <!-- 002 claude
//                                // В этом месте данные в curPlayingRadioStation будут старые, т.е. данные о предыдущей радиостанции. Это нужно для сравнения предыдущей и текущей в дальнейшем в методе mainViewModel.playOrToggleSong()
//                                switchViewPagerToCurrentSong(
//                                    curPlayingRadioStation?.stationuuid ?: return@observe,
//                                    curPlayingRadioStation?.countryCode ?: return@observe
//                                )

                                    // Полоса progressBar, которая заполняется с помощью Broadcast
                                    binding?.progressBarHorizontalDp?.progress = 85
                                    Log.d(
                                        TAG,
                                        "BROADCAST: Заполняем полосу прогресса на 85% в mediaItemsListLiveData.observe()"
                                    )
//                                002 claude -->

                                    Log.d(
                                        TAG,
                                        "PLAYLIST_UPDATE: 4.$TAG. Получаем данные из mediaItemsListLiveData"
                                    )

                                    mOnPageChangeCallback?.onPageScrolled(0, 0.0f, 0)
                                    // ??? Если не включать плейер, а просто листать от списка к списку, этот метод перестаёт вызываться на четвертый раз и звезда перестаёт меняться (избранное/не избранное). Поэтому на всякий случай вызываю его дополнительно. Не самый лучший вариант, думаю. Поэтому помечаю на проверку в дальнейшем.

//                                <!-- 002 claude
//                                // Полоса progressBar, которая заполняется с помощью Broadcast
//                                binding?.progressBarHorizontalDp?.progress = 85
//                                Log.d(
//                                    TAG,
//                                    "BROADCAST: Заполняем полосу прогресса на 85% в mediaItemsListLiveData.observe()"
//                                )

                                    // Возвращаем ViewPager на станцию, которая сейчас в плейере. Берём её из метаданных, а не из curPlayingRadioStation:
                                    // onPageSelected(0) выше уже записал туда первую станцию списка, и при повторной доставке того же списка
                                    // (при запуске он приходит дважды) плейер внизу показывал первую станцию вместо той, что на паузе/играет
                                    val curPlayingSong = mainViewModel.curPlayingSongLiveData.value
                                    switchViewPagerToCurrentSong(
                                        curPlayingSong?.mediaId
                                            ?: return@submitRadioStationList,
                                        curPlayingSong.mediaMetadata.subtitle.toString()
                                    )
                                }
//                                002 claude -->
                            } else {
                                // Список radioStations пуст. Бродкаст на это не срабатывает
                                // TODO а если это список избранного? Ему можно быть пустым
                            }

                        }

                        mainViewModel.stateInitialized()
                        runPendingWhenPlaylistReady()

                    }

                    ERROR -> Unit // we don't need this
                    LOADING -> Unit // we don't need this
                }
            }
        }

//        // 3. PROGRESS Подписываемся на состояние загрузки
//        mainViewModel.playlistLoadState.observe(this) { state ->
//            Log.d("UI_DEBUG", "PLAYER_DEBUG: LoadState update. Loading: ${state.loading}, playerReady: ${state.playerReady}, playlistId: ${state.playlistId}")
//            // Проверяем, что состояние актуально
//            if (state.playlistId != currentPlaylistId && currentPlaylistId != -1) {
//                return@observe
//            }
//
//            // Обновляем UI в соответствии с состоянием
////            binding.frameLayoutDp.isVisible = state.loading
////            binding.progressBarHorizontalDp.isVisible = state.loading
////            swipeRadioStationAdapter.isClickableRecyclerView = !state.loading
//
//            if (state.loading){
//                mainViewModel.showProgressAndDisableClick() //TODO сделать тест просто "Загрузка..."
//            } else{
//                mainViewModel.hideProgressAndSetClickable()
//            }
//
//            // Если загрузка завершена и плеер готов
//            if (!state.loading && state.playerReady) {
//                // Дополнительные действия после успешной загрузки
//                Log.d("UI_DEBUG", "PLAYER_DEBUG: Player ready! Should be playing now.")
//            }
//        }

        // LIVEDATA: every time we have new info about currently playing song (when the song switches)
        mainViewModel.curPlayingSongLiveData.observe(this) {
            if (it == null) return@observe

            whenPlaylistReady {
                run {

                    // Полоса progressBar, которая заполняется с помощью Broadcast
                    binding?.progressBarHorizontalDp?.progress = 95
                    Log.d(
                        TAG,
                        "BROADCAST: Заполняем полосу прогресса на 95% в лямбде whenReady{} из curPlayingSongLiveData.observe()"
                    )

                    // if we had an individual image
//            glide.load(curPlayingSong?.imageUrl).into(ivCurSongImage)

                    val mediaId = it.mediaId
                    val countrycode = it.mediaMetadata.subtitle.toString()
//                            switchViewPagerToCurrentSong(mediaId ?: return@observe, countrycode)
                    switchViewPagerToCurrentSong(mediaId ?: return@run, countrycode)

                    Log.d(
                        TAG,
                        "BROADCAST: Прячем прогресс - curPlayingSongLiveData.observe. Если список радиостаниций не пуст и countrycode одинаковый в vpSong и плейере, скроется прогресс"
                    )

                    // Favorite star
                    if (swipeRadioStationAdapter.radioStationList.isNotEmpty()) {
                        checkFavoriteStarIfCountryCodeIsRight(
                            countrycode,
                            swipeRadioStationAdapter.radioStationList[0].countryCode
                        )
                    }

                }
            }

        }

//        mainViewModel.updateCurPlayingRadioStationLiveData.observe(this) {
//            curPlayingRadioStation = it
//        }

        // Иногда сбивается и в уведомлении показывает правильную станцию, а в плейере - нет. Добавляю страховку
        mainViewModel.switchViewPagerOnceAgainLiveData.observe(this) {
            whenPlaylistReady {
                switchViewPagerToCurrentSong(it.stationuuid, it.countryCode)
            }
        }

        // LIVEDATA: Will be called everytime the playback changes (pause the player, play a song etc.) -> change our image
        mainViewModel.playbackStateLiveData.observe(this) {
            playbackState = it
            binding?.ivPlayPause?.setImageResource(
                if (playbackState?.isPlaying == true) R.drawable.ic_pause_orange else R.drawable.ic_play_arrow_orange
            )

//            <!-- 004 claude
            // "Connecting to radio station" прячем, как только плейер после её показа начал играть или сообщил об ошибке
            // (ошибка - это тот же момент, когда сервис показывает toast). Ждать смены метаданных нельзя:
            // при повторном выборе той же станции (например, после ошибки) метаданные не меняются, и полоса висела до таймаута
            val connectingShownAt = mainViewModel.connectingProgressShownAt
            if (connectingShownAt != null && it != null && it.updateTime >= connectingShownAt &&
                (it.isActuallyPlaying || it.hasError)
            ) {
                Log.d(
                    TAG,
                    "BROADCAST: Прячем Connecting to radio station, состояние плейера = $it"
                )
                mainViewModel.hideProgressAndSetClickable()
            }
            // 004 claude -->

        }

        // LIVEDATA: This event can be emitted once. We handled it in the class Event
        mainViewModel.isConnectedLiveData.observe(this) {
            // The first time .getContentIfNotHandled() is handled, it will return the type boolean. But after that it will return null (the second time, on the same object)
            it?.getContentIfNotHandled()?.let { result ->
                when (result.status) {
                    // If everything is ok, we don't want to show anything. Only if smth went wrong
                    ERROR -> {
                        Log.d(
                            TAG,
                            "An unknown error occurred in mainViewModel.isConnectedLiveData.observe"
                        )
                        binding?.let { nonNullBinding ->
                            Snackbar.make(
                                nonNullBinding.rootLayout.rootView,
                                result.message ?: "An unknown error occurred",
                                Snackbar.LENGTH_LONG
                            ).show()

                            // Убираем прогресс и делаем кнопки снова кликабельными
                            mainViewModel.hideProgressAndSetClickable()
                        }
                    }

                    else -> Unit
                }
            }
        }

        // LIVEDATA: when error
        mainViewModel.networkErrorLiveData.observe(this) {
            it?.getContentIfNotHandled()?.let { result ->
                when (result.status) {
                    // If everything is ok, we don't want to show anything. Only if smth went wrong
                    ERROR -> {
                        Log.d(
                            TAG,
                            "An unknown error occurred in mainViewModel.networkErrorLiveData.observe"
                        )
                        binding?.let { nonNullBinding ->
                            Snackbar.make(
                                nonNullBinding.rootLayout.rootView,
                                result.message ?: "An unknown error occurred",
                                Snackbar.LENGTH_LONG
                            ).show()

                            // Убираем прогресс и делаем кнопки снова кликабельными
                            mainViewModel.hideProgressAndSetClickable()
                        }
                    }

                    else -> Unit
                }
            }
        }

        mainViewModel.errorMessageLiveData.observe(this) {
            it?.getContentIfNotHandled()?.let { result ->
                when (result.status) {
                    // If everything is ok, we don't want to show anything. Only if smth went wrong
                    ERROR -> {
                        Log.d(
                            TAG,
                            "An unknown error occurred in mainViewModel.errorMessageLiveData.observe"
                        )
                        binding?.let { nonNullBinding ->
                            Snackbar.make(
                                nonNullBinding.rootLayout.rootView,
                                result.message ?: "An unknown error occurred",
                                Snackbar.LENGTH_LONG
                            ).show()

                            // Убираем прогресс и делаем кнопки снова кликабельными
                            mainViewModel.hideProgressAndSetClickable()
                        }
                    }

                    else -> Unit
                }
            }
        }

        mainViewModel.messageLiveData.observe(this) {
            when (it) {
//                AUDIO_CONNECTING -> Toast.makeText(
//                    this,
//                    it,
//                    Toast.LENGTH_LONG
//                ).show()
//
//                AUDIO_STOPPED, AUDIO_PLAYING -> Toast.makeText(
//                    this,
//                    it,
//                    Toast.LENGTH_SHORT
//                ).show()

                AUDIO_CONNECTING -> binding?.let { nonNullBinding ->
                    Snackbar.make(
                        nonNullBinding.rootLayout.rootView,
                        it,
                        Snackbar.LENGTH_LONG
                    ).show()
                }

                AUDIO_STOPPED, AUDIO_PLAYING -> binding?.let { nonNullBinding ->
                    Snackbar.make(
                        nonNullBinding.rootLayout.rootView,
                        it,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }

                else -> {
                } // Note the block
            }
        }

        // Favourites
        // <!-- 007 claude
//        mainViewModel.stationSavedInFavouritesLiveData.observe(this) {
//            binding?.imageStar?.setImageResource(R.drawable.ic_baseline_star_24_orange)
//            // Так же ставим true в объекте текущей радиостанции
//            setTheRightStateOfFavourite(true)
//        }
//        mainViewModel.stationDeletedFromFavouritesLiveData.observe(this) {
//            binding?.imageStar?.setImageResource(R.drawable.ic_baseline_star_border_24_orange)
//            // Так же ставим false в объекте текущей радиостанции
//            setTheRightStateOfFavourite(false)
//        }
//
//        // Если изменение было в FavouriteListFragment, здесь тоже нужно это отобразить:
//        mainViewModel.changeTheStarLiveData.observe(this) {
//            if (it) {
//                binding?.imageStar?.setImageResource(R.drawable.ic_baseline_star_24_orange)
//            } else {
//                binding?.imageStar?.setImageResource(R.drawable.ic_baseline_star_border_24_orange)
//            }
//        }

        // Favourites: звезда нажата где угодно (в плейере, списке избранного, рекомендованных).
        // Меняем признак у ЭТОЙ станции в плейлисте плейера, а звезду в плейере - только если показана именно она
        val skipFavouriteChangesUpToId = mainViewModel.lastFavouriteChangeIdForNewObserver
        mainViewModel.favouriteChangeLiveData.observe(this) { change ->
            if (change.id <= skipFavouriteChangesUpToId) return@observe

            val radioStationList = swipeRadioStationAdapter.radioStationList
            radioStationList.forEach {
                if (it.stationuuid == change.station.stationuuid) it.isStationInFavourite = change.isFavourite
            }

            val shownStation = radioStationList.getOrNull(binding?.vpSong?.currentItem ?: -1)
            if (shownStation?.stationuuid == change.station.stationuuid) {
                binding?.imageStar?.setImageResource(
                    if (change.isFavourite) R.drawable.ic_baseline_star_24_orange else R.drawable.ic_baseline_star_border_24_orange
                )
            }
        }
        // 007 claude -->

        mainViewModel.setNonClickableDpLiveData.observe(this) {
            // Изредка не срабатывает логика и кнопки остаются заблокированым. В таком случае нет возможности продолжать пользоваться приложением.
//            // Запустить отображение прогресс бара + заблокировать нажатия как на HomeRadioFragment, так и проигрыватель в main activity
//            // MainActivity
//            binding?.imageStar?.isClickable = false
//            binding?.imageStar?.isEnabled = false
//            binding?.vpSong?.isClickable = false // не работает
//            binding?.vpSong?.isEnabled = false // не работает
//            binding?.ivPlayPause?.isClickable = false
//            binding?.ivPlayPause?.isEnabled = false
            swipeRadioStationAdapter.isClickableRecyclerView = false

            binding?.frameLayoutDp?.isVisible = true
            binding?.text1Dp?.isVisible = true
            // Возвращаем текст
            val textForConnecting = getString(R.string.downloading_playlist)
            binding?.text1Dp?.text = textForConnecting
            binding?.progressBarHorizontalDp?.isVisible = true
        }
        mainViewModel.setNonClickableCRStLiveData.observe(this) {
            swipeRadioStationAdapter.isClickableRecyclerView = false

            binding?.frameLayoutDp?.isVisible = true
            binding?.text1Dp?.isVisible = true
            // Меняем текст
            val textForConnecting = getString(R.string.downloading_radio_station)
            binding?.text1Dp?.text = textForConnecting
            binding?.progressBarHorizontalDp?.isVisible = true
        }
//        mainViewModel.setNonClickableLiveData.observe(this) {
//            swipeRadioStationAdapter.isClickableRecyclerView = false
//
//            binding?.frameLayoutDp?.isVisible = true
//            binding?.text1Dp?.isVisible = true
//            // Возвращаем текст
//            val textForConnecting = getString(R.string.connecting)
//            binding?.text1Dp?.text = textForConnecting
//            binding?.progressBarHorizontalDp?.isVisible = true
//        }
        mainViewModel.setClickableLiveData.observe(this) {
//            // Убрать отображение прогресс бара + разблокировать нажатия как на HomeRadioFragment, так и проигрыватель в main activity
//            // MainActivity
//            binding?.imageStar?.isClickable = true
//            binding?.imageStar?.isEnabled = true
//            binding?.vpSong?.isClickable = true // не работает
//            binding?.vpSong?.isEnabled = true // не работает
//            binding?.ivPlayPause?.isClickable = true
//            binding?.ivPlayPause?.isEnabled = true
            swipeRadioStationAdapter.isClickableRecyclerView = true

            binding?.frameLayoutDp?.isVisible = false
            binding?.text1Dp?.isVisible = false
            binding?.progressBarHorizontalDp?.isVisible = false
        }

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

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
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
                            "BROADCAST: Прячем прогресс. Вызываем метод hideProgressAndSetClickable() из curPlayingSongLiveData.observe"
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

    // <!-- 007 claude
//    private fun setTheRightStateOfFavourite(isInFavourite: Boolean) {
//        if (swipeRadioStationAdapter.radioStationList.isNotEmpty()) {
//            val currentRadioStationPosition = binding?.vpSong?.currentItem
//
//            currentRadioStationPosition?.let {
//                swipeRadioStationAdapter.radioStationList[it].isStationInFavourite = isInFavourite
//
//                if (isInFavourite) {
//                    mainViewModel.addAStationToFavouriteListIfItIsNotThere(swipeRadioStationAdapter.radioStationList[it])
//                }
//            }
//        }
//    }
    // 007 claude -->

    private fun showCustomDialog(titleId: Int, textId: Int) {
        val titleInternetTrouble = getString(titleId)
        val titleViewInternetTrouble =
            dialogPleaseWait.findViewById<AppCompatTextView>(R.id.title_pleaseWait)
        titleViewInternetTrouble.text = titleInternetTrouble

        val textInternetTrouble = getString(textId)
        val textViewInternetTrouble =
            dialogPleaseWait.findViewById<AppCompatTextView>(R.id.text_pleaseWait)
        textViewInternetTrouble.text = textInternetTrouble

        dialogPleaseWait.show()
    }

    // function for hiding our bottom bar
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

//    override fun onBackPressed() {
//        super.onBackPressed()
//    }

    override fun setToolbar(toolbar: Toolbar?) {
        setSupportActionBar(toolbar)
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
        dialogPleaseWait.dismiss() // Открытый диалог закрываем вместе с Activity, иначе WindowLeaked

        binding = null // VIEW BINDING -> 3. onDestroyView()

//        // 1.Broadcast для того, чтобы убрать уведомление (2,3 - в MusicService)
//        val intentMS =
//            Intent(FILTER_FOR_BROADCAST_MS) // FILTER is a string to identify this intent
//        intentMS.apply {
//            Log.d(TAG, "Отправляем ключ KEY_BROADCAST_ACTIVITY, чтобы убрать уведомление")
//            putExtra(KEY_BROADCAST_ACTIVITY, 100)
//            sendBroadcast(this)
//        }

        super.onDestroy()
    }

    // 2.Broadcast для полосы прогресса MainActivity при загрузке плейлиста (1 - в ???)
    // Создадим анонимный класс => не нужно регистрировать в манифесте
    private var receiver: BroadcastReceiver? = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {

            Log.d(TAG, "BROADCAST: Получаем данные в onReceive()")

            // Общее количество станций
            val listSize = intent.getIntExtra(Constants.KEY_BROADCAST_LIST_SIZE_MA, 1)
            // Какая по счету обрабатывается сейчас в FirebaseMusicSource
            val filesAmount = intent.getIntExtra(Constants.KEY_BROADCAST_COUNT_MA, 1)

            if (listSize > 0 && filesAmount <= listSize) { // listSize = 0 - пустой плейлист, без проверки было бы деление на ноль
                val progress = 70 * filesAmount / listSize
                binding?.progressBarHorizontalDp?.progress = progress
                Log.d(
                    TAG,
                    "BROADCAST: Получаем данные в onReceive(). progress = 70 * $filesAmount / $listSize = $progress%"
                )
            }

        }
    }

    private val receiverServerIsDown: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            // Ваш код обработки сообщения
            Log.d(TAG, "BROADCAST: Получаем данные в onReceive() receiverServerIsDown")

            val isServerDown = intent.getBooleanExtra(KEY_BROADCAST_SERVER_IS_DOWN, false)

            if (isServerDown) {
                showCustomDialog(
                    R.string.dialogInternetTrouble_title4,
                    R.string.dialogInternetTrouble_text4
                )
                mainViewModel.hideProgressAndSetClickable(true)
            }
        }
    }
}