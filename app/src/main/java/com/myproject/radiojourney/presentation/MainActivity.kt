package com.myproject.radiojourney.presentation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.Toast
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
import com.myproject.radiojourney.other.Constants.AUDIO_CONNECTING
import com.myproject.radiojourney.other.Constants.AUDIO_PLAYING
import com.myproject.radiojourney.other.Constants.AUDIO_STOPPED
import com.myproject.radiojourney.other.Status.*
import com.myproject.radiojourney.presentation.content.radioStationList.adapter.SwipeRadioStationAdapter
import com.myproject.radiojourney.presentation.content.homeRadio.HomeRadioFragmentDirections
import com.myproject.radiojourney.utils.extension.isPlaying
import com.myproject.radiojourney.utils.service.ProgressForegroundService
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
    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: ActivityMainBinding? = null

    private val mainViewModel by viewModels<MainViewModel>() // Такую же view model мы зарегистрировали в homeFragment. Основная ViewModel, для общения с плейером в bottom bar

    // Variable for currently playing song
    private var curPlayingRadioStation: RadioStationPresentation? = null
    private var playbackState: PlaybackStateCompat? = null

    private var mOnPageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private val swipeRadioStationAdapter = SwipeRadioStationAdapter()

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
                if (swipeRadioStationAdapter.radioStationList.isNotEmpty()) {

                    if (swipeRadioStationAdapter.radioStationList[position].isStationInFavourite) {
                        binding?.imageStar?.setImageResource(R.drawable.ic_baseline_star_24_orange)
                    } else {
                        binding?.imageStar?.setImageResource(R.drawable.ic_baseline_star_border_24_orange)
                    }

                }
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }

            // function, that is called when the viewpager is swiped - onPageSelected()
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                mainViewModel.synchronizedCheckThePosition(
                    position,
                    mainViewModel.mediaItemsListLiveData.value?.data,
                    swipeRadioStationAdapter.radioStationList
                )
            }
        }

        initListeners()
        subscribeToObservers()

        // COUNTRY LIST MARKERS ON MAP -> 1. Получаем список кодов стран, преобразуем в локальные модели, сохраняем в Room.
        // Делается 1 раз, при запуске приложения и по окончанию stopSelf()
        this.startService(
            Intent(
                this,
                ProgressForegroundService::class.java
            )
        )
    }

    private fun initListeners() {
        // To detect if it is swiped
        mOnPageChangeCallback?.let {
            binding?.vpSong?.registerOnPageChangeCallback(it)
        }

        // Click listener (on play image)
        binding?.ivPlayPause?.setOnClickListener {
            curPlayingRadioStation?.let {
                mainViewModel.playOrToggleSong(it, true) // true, because now we want to autoplay
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
        var radioStationNeedToFind: RadioStationPresentation? = null

        if (swipeRadioStationAdapter.radioStationList.isNotEmpty()) {
            swipeRadioStationAdapter.radioStationList.forEach {
                if (it.urlResolved == mediaId) {
                    radioStationNeedToFind = it
                }
            }
        }

        radioStationNeedToFind?.let {
            val newItemIndex =
                swipeRadioStationAdapter.radioStationList.indexOf(radioStationNeedToFind) // looking for the index of that song

            // That function will return -1 if the song doesn't exist, so we must check:
            if (newItemIndex != -1) {
                binding?.vpSong?.currentItem =
                    newItemIndex // currentItem - is the index of the song, that is displayed. We change it to a new one

                curPlayingRadioStation =
                    radioStationNeedToFind // we also update our curPlayingRadioStation
            }

            // Убираем прогресс и делаем кнопки снова кликабельными
            mainViewModel.hideProgressAndSetClickable()
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

                            // Попробуем назначить адаптер после обновления списка радиостанций
                            binding?.vpSong?.adapter = swipeRadioStationAdapter

                            mOnPageChangeCallback?.onPageSelected(0)
                            // TODO почему-то этот метод изредка не вызывается, хотя должен

                            // В этом месте данные в curPlayingRadioStation будут старые, т.е. данные о предыдущей радиостанции. Это нужно для сравнения предыдущей и текущей в дальнейшем в методе mainViewModel.playOrToggleSong()

                            switchViewPagerToCurrentSong(
                                curPlayingRadioStation?.urlResolved ?: return@observe,
                                curPlayingRadioStation?.countryCode ?: return@observe
                            )

                            mOnPageChangeCallback?.onPageScrolled(0, 0.0f, 0)
                            // TODO Если не включать плейер, а просто листать от списка к списку, этот метод перестаёт вызываться на четвертый раз и звезда перестаёт меняться (избранное/не избранное). Поэтому на всякий случай вызываю его дополнительно. Не самый лучший вариант, думаю. Поэтому помечаю на проверку в дальнейшем.
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

//            curPlayingRadioStation =
//                it.toRadioStationPresentation() // we use a method from our extensions (to convert Media MetadataCompat to our RadioStationPresentation)

            // if we had an individual image
//            glide.load(curPlayingSong?.imageUrl).into(ivCurSongImage)

            val mediaId = it.description.mediaId

            switchViewPagerToCurrentSong(
                mediaId ?: return@observe,
                it.description.subtitle.toString()
            )

            if (swipeRadioStationAdapter.radioStationList.isNotEmpty() && it.description?.subtitle == swipeRadioStationAdapter.radioStationList[0].countryCode) {
                val currentRadioStationPosition = binding?.vpSong?.currentItem

                currentRadioStationPosition?.let { position ->
                    if (swipeRadioStationAdapter.radioStationList[position].isStationInFavourite) {
                        binding?.imageStar?.setImageResource(R.drawable.ic_baseline_star_24_orange)
                    } else {
                        binding?.imageStar?.setImageResource(R.drawable.ic_baseline_star_border_24_orange)
                    }
                }

                // Убираем прогресс и делаем кнопки снова кликабельными
                mainViewModel.hideProgressAndSetClickable()
            }
        }

        mainViewModel.updateCurPlayingRadioStationLiveData.observe(this) {
            curPlayingRadioStation = it
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
                                result.message ?: "An unknown error occurred",
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
                                result.message ?: "An unknown error occurred",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    else -> Unit
                }
            }
        }

        mainViewModel.errorMessageLiveData.observe(this) {
            it?.getContentIfNotHandled()?.let { result ->
                when (result.status) {
                    // If everything is ok, we don't want to show anything. Only if smth went wrong
                    ERROR ->
                        binding?.let { nonNullBinding ->
                            Snackbar.make(
                                nonNullBinding.rootLayout.rootView,
                                result.message ?: "An unknown error occurred",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    else -> Unit
                }
            }
        }

        mainViewModel.messageLiveData.observe(this) {
            when (it) {
                AUDIO_CONNECTING -> Toast.makeText(
                    this,
                    it,
                    Toast.LENGTH_LONG
                ).show()
                AUDIO_STOPPED, AUDIO_PLAYING -> Toast.makeText(
                    this,
                    it,
                    Toast.LENGTH_SHORT
                ).show()
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

        mainViewModel.setNonClickableLiveData.observe(this) {
            // Изредка не срабатывает логика и кнопки остаются заблокированым. В таком случае нет возможности продолжать пользоваться приложением.
//            // Запустить отображение прогресс бара + заблокировать нажатия как на HomeRadioFragment, так и проигрыватель в main activity
//            // MainActivity
//            binding?.imageStar?.isClickable = false
//            binding?.imageStar?.isEnabled = false
//            binding?.vpSong?.isClickable = false // TODO не работает
//            binding?.vpSong?.isEnabled = false // TODO не работает
//            binding?.ivPlayPause?.isClickable = false
//            binding?.ivPlayPause?.isEnabled = false
        }
        mainViewModel.setClickableLiveData.observe(this) {
//            // Убрать отображение прогресс бара + разблокировать нажатия как на HomeRadioFragment, так и проигрыватель в main activity
//            // MainActivity
//            binding?.imageStar?.isClickable = true
//            binding?.imageStar?.isEnabled = true
//            binding?.vpSong?.isClickable = true // TODO не работает
//            binding?.vpSong?.isEnabled = true // TODO не работает
//            binding?.ivPlayPause?.isClickable = true
//            binding?.ivPlayPause?.isEnabled = true
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

    override fun onDestroy() {
        mOnPageChangeCallback?.let {
            binding?.vpSong?.unregisterOnPageChangeCallback(it)
        }

        binding = null // VIEW BINDING -> 3. onDestroyView()

        super.onDestroy()
    }

    override fun setToolbar(toolbar: Toolbar?) {
        setSupportActionBar(toolbar)
    }
}