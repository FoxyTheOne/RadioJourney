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
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
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
import com.myproject.radiojourney.other.Status.ERROR
import com.myproject.radiojourney.other.Status.LOADING
import com.myproject.radiojourney.other.Status.SUCCESS
import com.myproject.radiojourney.presentation.content.homeRadio.HomeRadioFragmentDirections
import com.myproject.radiojourney.presentation.content.radioStationList.adapter.SwipeRadioStationAdapter
import com.myproject.radiojourney.utils.extension.isPlaying
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
    private var playbackState: PlaybackStateCompat? = null

    private var mOnPageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private val swipeRadioStationAdapter = SwipeRadioStationAdapter()

    private lateinit var dialogPleaseWait: Dialog
    private var isInternetAvailable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        setContentView(R.layout.activity_main) <- заменяем на view binding:
        // VIEW BINDING -> 2. Инициализация
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view: View = binding!!.root
        setContentView(view)

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
                        mainViewModel.curPlayingSongLiveData.value?.description?.subtitle.toString(),
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
                        mainViewModel.curPlayingSongLiveData.value?.description?.subtitle.toString(),
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
                val titleInternetTrouble = getString(R.string.dialogInternetTrouble_title)
                val textInternetTrouble = getString(R.string.dialogInternetTrouble_text3)
                val titleViewInternetTrouble =
                    dialogPleaseWait.findViewById<AppCompatTextView>(R.id.title_pleaseWait)
                val textViewInternetTrouble =
                    dialogPleaseWait.findViewById<AppCompatTextView>(R.id.text_pleaseWait)
                titleViewInternetTrouble.text = titleInternetTrouble
                textViewInternetTrouble.text = textInternetTrouble

                dialogPleaseWait.show()
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
        val curPlayingMediaId = mainViewModel.curPlayingSongLiveData.value?.description?.mediaId
        if (mediaId != curPlayingMediaId) return

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
                when (result.status) {
                    SUCCESS -> {

                        result.data?.let { radioStations ->

                            if (radioStations.isNotEmpty()) {
                                swipeRadioStationAdapter.radioStationList = radioStations
                                // if we had an individual image
//                            if(radioStations.isNotEmpty()) {
//                                glide.load((curPlayingSong ?: radioStations[0]).imageUrl).into(ivCurSongImage)
//                            }

                                // Попробуем назначить адаптер после обновления списка радиостанций
                                binding?.vpSong?.adapter = swipeRadioStationAdapter

                                mOnPageChangeCallback?.onPageSelected(0)
                                // Почему-то этот метод изредка не вызывается, хотя должен. На всякий случай дублирую вызов здесь

                                // В этом месте данные в curPlayingRadioStation будут старые, т.е. данные о предыдущей радиостанции. Это нужно для сравнения предыдущей и текущей в дальнейшем в методе mainViewModel.playOrToggleSong()
                                switchViewPagerToCurrentSong(
                                    curPlayingRadioStation?.stationuuid ?: return@observe,
                                    curPlayingRadioStation?.countryCode ?: return@observe
                                )

                                Log.d(
                                    TAG,
                                    "PLAYLIST_UPDATE: 4.$TAG. Получаем данные из mediaItemsListLiveData"
                                )

                                mOnPageChangeCallback?.onPageScrolled(0, 0.0f, 0)
                                // ??? Если не включать плейер, а просто листать от списка к списку, этот метод перестаёт вызываться на четвертый раз и звезда перестаёт меняться (избранное/не избранное). Поэтому на всякий случай вызываю его дополнительно. Не самый лучший вариант, думаю. Поэтому помечаю на проверку в дальнейшем.

                                // Полоса progressBar, которая заполняется с помощью Broadcast
                                binding?.progressBarHorizontalDp?.progress = 85
                                Log.d(
                                    TAG,
                                    "BROADCAST: Заполняем полосу прогресса на 85% в mediaItemsListLiveData.observe()"
                                )
                            }

                        }

                        mainViewModel.stateInitialized()

                    }

                    ERROR -> Unit // we don't need this
                    LOADING -> Unit // we don't need this
                }
            }
        }

        // LIVEDATA: every time we have new info about currently playing song (when the song switches)
        mainViewModel.curPlayingSongLiveData.observe(this) {
            if (it == null) return@observe

            mainViewModel.whenReady { isInitialized ->
                if (isInitialized) {

                    // Полоса progressBar, которая заполняется с помощью Broadcast
                    binding?.progressBarHorizontalDp?.progress = 95
                    Log.d(
                        TAG,
                        "BROADCAST: Заполняем полосу прогресса на 95% в лямбде whenReady{} из curPlayingSongLiveData.observe()"
                    )

                    // if we had an individual image
//            glide.load(curPlayingSong?.imageUrl).into(ivCurSongImage)

                    val mediaId = it.description.mediaId
                    val countrycode = it.description.subtitle.toString()
//                            switchViewPagerToCurrentSong(mediaId ?: return@observe, countrycode)
                    switchViewPagerToCurrentSong(mediaId ?: return@whenReady, countrycode)

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
            mainViewModel.whenReady { isInitialized ->
                if (isInitialized) {

                    switchViewPagerToCurrentSong(it.stationuuid, it.countryCode)

                }
            }
        }

        // LIVEDATA: Will be called everytime the playback changes (pause the player, play a song etc.) -> change our image
        mainViewModel.playbackStateLiveData.observe(this) {
            playbackState = it
            binding?.ivPlayPause?.setImageResource(
                if (playbackState?.isPlaying == true) R.drawable.ic_pause_orange else R.drawable.ic_play_arrow_orange
            )

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
        mainViewModel.stationSavedInFavouritesLiveData.observe(this) {
            binding?.imageStar?.setImageResource(R.drawable.ic_baseline_star_24_orange)
            // Так же ставим true в объекте текущей радиостанции
            setTheRightStateOfFavourite(true)
        }
        mainViewModel.stationDeletedFromFavouritesLiveData.observe(this) {
            binding?.imageStar?.setImageResource(R.drawable.ic_baseline_star_border_24_orange)
            // Так же ставим false в объекте текущей радиостанции
            setTheRightStateOfFavourite(false)
        }

        // Если изменение было в FavouriteListFragment, здесь тоже нужно это отобразить:
        mainViewModel.changeTheStarLiveData.observe(this) {
            if (it) {
                binding?.imageStar?.setImageResource(R.drawable.ic_baseline_star_24_orange)
            } else {
                binding?.imageStar?.setImageResource(R.drawable.ic_baseline_star_border_24_orange)
            }
        }

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

    private fun setTheRightStateOfFavourite(isInFavourite: Boolean) {
        if (swipeRadioStationAdapter.radioStationList.isNotEmpty()) {
            val currentRadioStationPosition = binding?.vpSong?.currentItem

            currentRadioStationPosition?.let {
                swipeRadioStationAdapter.radioStationList[it].isStationInFavourite = isInFavourite

                if (isInFavourite) {
                    mainViewModel.addAStationToFavouriteListIfItIsNotThere(swipeRadioStationAdapter.radioStationList[it])
                }
            }
        }
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

    override fun onDestroy() {
        mOnPageChangeCallback?.let {
            binding?.vpSong?.unregisterOnPageChangeCallback(it)
        }

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
            val listSize = intent.getIntExtra(Constants.KEY_BROADCAST_LIST_SIZE_MA, 1)
            val filesAmount = intent.getIntExtra(Constants.KEY_BROADCAST_COUNT_MA, 1)

            val isCountryCodeRemoteListEmpty =
                intent.getBooleanExtra(
                    Constants.KEY_BROADCAST_IS_EMPTY_MA,
                    false
                ) // Не тот бродкаст, удалить
//            val endOfBroadcast = intent.getIntExtra(Constants.KEY_BROADCAST_END_MA, 1)

            Log.d(TAG, "BROADCAST: Получаем данные в onReceive()")

            if (filesAmount <= listSize) {
                val progress = 70 * filesAmount / listSize
                binding?.progressBarHorizontalDp?.progress = progress
                Log.d(
                    TAG,
                    "BROADCAST: Получаем данные в onReceive(). progress = 70 * $filesAmount / $listSize = $progress%"
                )
            }

//            // Не тот бродкаст, удалить
//            if (isCountryCodeRemoteListEmpty) {
//                // Меняем текст диалогового окна
//                val tittleSmthWentWrong = getString(R.string.dialogPleaseWait_title2)
//                val textSmthWentWrong = getString(R.string.dialogPleaseWait_text2)
//                val tittleViewSmthWentWrong = dialogPleaseWait.findViewById<AppCompatTextView>(R.id.title_pleaseWait)
//                val textViewSmthWentWrong = dialogPleaseWait.findViewById<AppCompatTextView>(R.id.text_pleaseWait)
//                tittleViewSmthWentWrong.text = tittleSmthWentWrong
//                textViewSmthWentWrong.text = textSmthWentWrong
//
//                dialogPleaseWait.show()
//            }

//            // Когда прогресс заканчивается, отправляем об этом Broadcast
//            val intent =
//                Intent(Constants.FILTER_FOR_BROADCAST_MA) // FILTER is a string to identify this intent
//            intent.putExtra(Constants.KEY_BROADCAST_END_MA, 100)
//            Log.d(
//                TAG,
//                "BROADCAST: Отправляем в MainActivity сигнал об окончании бродкаста (95%)"
//            )
//            sendBroadcast(intent)

//            if (endOfBroadcast == 100) {
//                binding?.progressBarHorizontalDp?.progress = 100
//                Log.d(TAG, "BROADCAST: endOfBroadcast == 100, заполняем полосу полностью")
//            }
        }
    }
}