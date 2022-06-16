package com.myproject.radiojourney.presentation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.myproject.radiojourney.IAppSettings
import com.myproject.radiojourney.R
import com.myproject.radiojourney.databinding.ActivityMainBinding
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import com.myproject.radiojourney.other.Status.*
import com.myproject.radiojourney.presentation.adapter.SwipeRadioStationAdapter
import com.myproject.radiojourney.presentation.content.homeRadio.HomeRadioFragmentDirections
import com.myproject.radiojourney.utils.extension.isPlaying
import com.myproject.radiojourney.utils.extension.toRadioStationPresentation
import com.myproject.radiojourney.utils.musicPlayer.ForegroundNotificationService
import com.myproject.radiojourney.utils.service.ProgressForegroundService
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

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
 * - Для хранения небольших пар ключ-значение (логин и пароль, токен и тп.) я использую Shared preferences;
 * - Для сохранения локаций маркеров на карте, а также для хранения избранных радиостанций используется реляционная база данных Room.
 * При первом запуске нужно дождаться окончания кеширования, в дальнейшем данные берутся из подписки на локальную базу данных;
 * - Для отображения прогресса кеширования в уведомлении используется Foreground service;
 * - Все запросы на сервер, либо в локальную БД из ViewModel я делаю через Coroutines;
 * - Для запроса на сервер используется Retrofit2.
 *
 * My graduate work is an application for listening to Internet radio stations. I am using API.radio-browser.info
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
 * You need to wait for the end of caching at the first start. Further the data is taken from the subscription to the local database;
 * - I use Foreground service to display caching progress in notification;
 * - I make all requests to the server, or to the local database from the ViewModel, through Coroutines;
 * - For the request to the server, Retrofit2 is used.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), IAppSettings {
    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: ActivityMainBinding? = null

    private val mainViewModel by viewModels<MainViewModel>() // Такую же view model мы зарегистрировали в homeFragment. Основная ViewModel, для общения с плейером в bottom bar

    private val swipeRadioStationAdapter = SwipeRadioStationAdapter()

    // Variable for currently playing song
    private var curPlayingRadioStation: RadioStationPresentation? = null
    private var playbackState: PlaybackStateCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main) <- заменяем на view binding:
        // VIEW BINDING -> 2. Инициализация
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view: View = binding!!.root
        setContentView(view)

        binding?.vpSong?.adapter = swipeRadioStationAdapter

        subscribeToObservers()
        initListeners()

        // COUNTRY LIST MARKERS ON MAP -> 1. Получаем список кодов стран, преобразуем в локальные модели, сохраняем в Room.
        // Делается 1 раз, при запуске приложения и по окончанию stopSelf()
        this.startService(
            Intent(
                this,
                ProgressForegroundService::class.java
            )
        )

        // Starting foreground service (music notification)
        this.startService(
            Intent(
                this,
                ForegroundNotificationService::class.java
            )
        )
    }

    private fun initListeners() {
        // To detect if it is swiped
        binding?.vpSong?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            // function, that is called when the viewpager is swiped - onPageSelected()
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // We must check, if player is playing
                if (playbackState?.isPlaying == true) {
                    mainViewModel.playOrToggleSong(swipeRadioStationAdapter.radioStationList[position])
                } else {
                    curPlayingRadioStation = swipeRadioStationAdapter.radioStationList[position]
                }
            }
        })

        // Click listener (on play image)
        binding?.ivPlayPause?.setOnClickListener {
            curPlayingRadioStation?.let {
                mainViewModel.playOrToggleSong(it, true) // true, because now we want to autoplay
            }
        }

        // Navigate to the RadioListFragment if a song in player was clicked
        swipeRadioStationAdapter.setItemClickListener {
            // Узнаем название страны
            // TODO Добавить "country" в RadioStationLocal и Presentation и брать эту строку оттуда
            val loc = Locale("", it.countryCode)
            val countryName = loc.displayName

            // Перенесём countryCode на RadioListFragment для запроса списка станций
            if (it.countryCode != "null") {
                val direction =
                    HomeRadioFragmentDirections.actionHomeRadioFragmentToRadioListFragment("${it.countryCode}||${countryName}")
                if (this.findNavController(R.id.navHostFragment).currentDestination?.id == R.id.homeRadioFragment) {
                    this.findNavController(R.id.navHostFragment).navigate(direction)
                }
            }
        }

        // Let's add a listener to our NavContoller to hide BottomBar when we are on the first page, where we are cashing
        this.findNavController(R.id.navHostFragment)
            .addOnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    R.id.firstScreenLoadingFragment -> hideBottomBar()
                    R.id.homeRadioFragment -> showBottomBar()
                    else -> showBottomBar()
                }
            }
    }

    // when a new song play, widget.ViewPager2 must automatically swipe to the corresponding song
    // parameter - the new song, that began to play
    private fun switchViewPagerToCurrentSong(radioStation: RadioStationPresentation) {
        val newItemIndex =
            swipeRadioStationAdapter.radioStationList.indexOf(radioStation) // looking for the index of that song
        // That function will return -1 if the song doesn't exist, so we must check:
        if (newItemIndex != -1) {
            binding?.vpSong?.currentItem =
                newItemIndex // currentItem - is the index of the song, that is displayed. We change it to a new one
            curPlayingRadioStation = radioStation // we also update our curPlayingRadioStation
        }
    }

    private fun subscribeToObservers() {
        // LIVEDATA: to fill our widget.ViewPager2 with correct items, display right ones WHEN WE LAUNCH OUR APP
        mainViewModel.mediaItemsListLiveData.observe(this) {
            it?.let { result ->
                when (result.status) {
                    SUCCESS -> {
                        result.data?.let { radioStations ->
                            swipeRadioStationAdapter.radioStationList = radioStations
                            // if we had an individual image
//                            if(radioStations.isNotEmpty()) {
//                                glide.load((curPlayingSong ?: radioStations[0]).imageUrl).into(ivCurSongImage)
//                            }
                            switchViewPagerToCurrentSong(curPlayingRadioStation ?: return@observe)
                        }
                    }
                    ERROR -> Unit // we don't need this
                    LOADING -> Unit // we don't need this
                }
            }
        }

        // LIVEDATA: every time we have new info about currently playing song (when the song switches)
        mainViewModel.curPlayingSongLiveData.observe(this) {
            if (it == null) return@observe

            curPlayingRadioStation =
                it.toRadioStationPresentation() // we use a method from our extensions (to convert Media MetadataCompat to our RadioStationPresentation)
            // if we had an individual image
//            glide.load(curPlayingSong?.imageUrl).into(ivCurSongImage)
            switchViewPagerToCurrentSong(curPlayingRadioStation ?: return@observe)
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
                    ERROR ->
                        binding?.let { nonNullBinding ->
                            Snackbar.make(
                                nonNullBinding.rootLayout.rootView,
                                result.message ?: "An unknown error occured",
                                Snackbar.LENGTH_LONG
                            ).show()
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
                    ERROR ->
                        binding?.let { nonNullBinding ->
                            Snackbar.make(
                                nonNullBinding.rootLayout.rootView,
                                result.message ?: "An unknown error occured",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    else -> Unit
                }
            }
        }
    }

    // function for hiding our bottom bar
    // TODO maybe use Group view?
    private fun hideBottomBar() {
        binding?.ivCurSongImage?.isVisible = false
        binding?.vpSong?.isVisible = false
        binding?.ivPlayPause?.isVisible = false
    }

    private fun showBottomBar() {
        binding?.ivCurSongImage?.isVisible = true
        binding?.vpSong?.isVisible = true
        binding?.ivPlayPause?.isVisible = true
    }

    override fun onDestroy() {
        // Stop foreground service (music notification)
        this.stopService(
            Intent(
                this,
                ForegroundNotificationService::class.java
            )
        )
        super.onDestroy()
        binding = null // VIEW BINDING -> 3. onDestroyView()
    }

    override fun setToolbar(toolbar: Toolbar?) {
        setSupportActionBar(toolbar)
    }
}