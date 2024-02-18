package com.myproject.radiojourney.presentation

import android.accounts.AccountsException
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.homeRadioUseCase.IHomeRadioUseCase
import com.myproject.radiojourney.domain.mainRadioUseCase.IMainRadioUseCase
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import com.myproject.radiojourney.other.Constants.ADD_SONGS
import com.myproject.radiojourney.other.Constants.MEDIA_ROOT_ID
import com.myproject.radiojourney.other.Event
import com.myproject.radiojourney.other.Resource
import com.myproject.radiojourney.utils.exoplayer.MusicServiceConnection
import com.myproject.radiojourney.utils.exoplayer.State
import com.myproject.radiojourney.utils.extension.call
import com.myproject.radiojourney.utils.extension.isPlayEnabled
import com.myproject.radiojourney.utils.extension.isPlaying
import com.myproject.radiojourney.utils.extension.isPrepared
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val musicServiceConnection: MusicServiceConnection,
    private val mainRadioInteractor: IMainRadioUseCase,
    private val homeRadioInteractor: IHomeRadioUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val _messageLiveData = MutableLiveData<String>()
    val messageLiveData: LiveData<String> = _messageLiveData

    private val _updateCurPlayingRadioStationLiveData =
        MutableLiveData<RadioStationPresentation>()
    val updateCurPlayingRadioStationLiveData: LiveData<RadioStationPresentation> =
        _updateCurPlayingRadioStationLiveData

    // LiveData contains the media data for our activity (our radioStationPresentationList)
    private val _mediaItemsListLiveData =
        MutableLiveData<Resource<List<RadioStationPresentation>>>()
    val mediaItemsListLiveData: LiveData<Resource<List<RadioStationPresentation>>> =
        _mediaItemsListLiveData

    // Saved to shared preference
    private val _dataSavedSuccessfulLiveData = MutableLiveData<Boolean>()
    val dataSavedSuccessfulLiveData: LiveData<Boolean> = _dataSavedSuccessfulLiveData

    // New mediaId for opening new playlist on a specific (chosen) position
    private val _newMediaIdLiveData = MutableLiveData<String>()
    val newMediaIdLiveData: LiveData<String> = _newMediaIdLiveData
    private val _newPositionLiveData = MutableLiveData<Int>()
    val newPositionLiveData: LiveData<Int> = _newPositionLiveData

    private val _isNotJustLaunchedLiveData = MutableLiveData<Boolean>()
    val isNotJustLaunchedLiveData: LiveData<Boolean> = _isNotJustLaunchedLiveData

    // Favourites
    private val _stationSavedInFavouritesLiveData = MutableLiveData<Boolean>()
    val stationSavedInFavouritesLiveData: LiveData<Boolean> = _stationSavedInFavouritesLiveData
    private val _stationDeletedFromFavouritesLiveData = MutableLiveData<Boolean>()
    val stationDeletedFromFavouritesLiveData: LiveData<Boolean> =
        _stationDeletedFromFavouritesLiveData

    private val _changeTheStarLiveData = MutableLiveData<Boolean>()
    val changeTheStarLiveData: LiveData<Boolean> =
        _changeTheStarLiveData
    private val _addAStationToFavouriteListIfItIsNotThereLiveData =
        MutableLiveData<RadioStationPresentation>()
    val addAStationToFavouriteListIfItIsNotThereLiveData: LiveData<RadioStationPresentation> =
        _addAStationToFavouriteListIfItIsNotThereLiveData

    private val _switchViewPagerOnceAgainLiveData = MutableLiveData<RadioStationPresentation>()
    val switchViewPagerOnceAgainLiveData: LiveData<RadioStationPresentation> =
        _switchViewPagerOnceAgainLiveData

    // LiveData from our ServiceConnection
    val isConnectedLiveData = musicServiceConnection.isConnectedLiveData
    val networkErrorLiveData = musicServiceConnection.networkErrorLiveData
    val playbackStateLiveData = musicServiceConnection.playbackStateLiveData
    val curPlayingSongLiveData = musicServiceConnection.curPlayingSongLiveData

    // If smth went wrong
    private val _errorMessageLiveData =
        MutableLiveData<Event<Resource<Boolean>>>() // It must be private, so that other classes can't change it
    val errorMessageLiveData: LiveData<Event<Resource<Boolean>>> =
        _errorMessageLiveData // And another LiveData, that equals to previous, so that classes can't change it

    private val _dialogInternetTroubleLiveData = MutableLiveData<Boolean>()
    val dialogInternetTroubleLiveData: LiveData<Boolean> =
        _dialogInternetTroubleLiveData

    private val _setClickableLiveData = MutableLiveData<Boolean>()
    val setClickableLiveData: LiveData<Boolean> = _setClickableLiveData
    private val _setNonClickableDpLiveData = MutableLiveData<Boolean>()
    val setNonClickableDpLiveData: LiveData<Boolean> = _setNonClickableDpLiveData
    private val _setNonClickableCRStLiveData = MutableLiveData<Boolean>()
    val setNonClickableCRStLiveData: LiveData<Boolean> = _setNonClickableCRStLiveData

    // Список лямбд action, которые будут передаваться в метод whenReady(), пока state == STATE_CREATED или state == STATE_INITIALIZING
    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    // Параметр state с setter для того, чтобы можно было привязать к этому параметру определенную логику
    private var state: State = State.STATE_CREATED // State on default
        set(value) {
            if (value == State.STATE_INITIALIZED || value == State.STATE_ERROR) {
                synchronized(onReadyListeners) { // synchronized for save change
                    field = value // sign a new value to the field
                    onReadyListeners.forEach { listener ->
                        listener(state == State.STATE_INITIALIZED) // go through list and call needed lambda function. If there will be STATE_ERROR instead STATE_INITIALIZED, we will get "false". So we can check, if it was successful or not
                    }
                }
            } else {
                field = value // if it is STATE_CREATED or STATE_INITIALIZING
            }
        }

    // A function which will add actions to our list of actions (returns boolean - if it is ready or not)
    fun whenReady(action: (Boolean) -> Unit): Boolean {
        return if (state == State.STATE_CREATED || state == State.STATE_INITIALIZING) {
            onReadyListeners += action // We are not ready, so just add action to list (we will do it later, when we will be ready)
            false // not ready
        } else {
            action(state == State.STATE_INITIALIZED) // we are ready, so we can call action
            true
        }
    }

    init {
        try {
            state = State.STATE_INITIALIZING

            // Here we start query media items, so let's put it into LiveData:
            _mediaItemsListLiveData.postValue(Resource.loading(null)) // Resource data loading status. Null as default - we don't have any data here yet. Т.е. мы кладём в _mediaItems LiveData значение - объект класса Resource с нужным нам флагом и данными

            musicServiceConnection.subscribe(
                MEDIA_ROOT_ID,
                object : MediaBrowserCompat.SubscriptionCallback() {
                    override fun onChildrenLoaded(
                        parentId: String,
                        children: MutableList<MediaBrowserCompat.MediaItem>
                    ) {
                        super.onChildrenLoaded(parentId, children)

                        viewModelScope.launch(Dispatchers.IO) {

                            // And here we convert children: MutableList<MediaBrowserCompat.MediaItem> to our format of data
                            // Данные подтягиваются из result.sendResult(firebaseMusicSource.asMediaItems()) в MusicService
                            val radioStationPresentationList =
                                mainRadioInteractor.mediaItemChildrenToRadioStationPresentation(
                                    children
                                )

                            _mediaItemsListLiveData.postValue(
                                Resource.success(
                                    radioStationPresentationList
                                )
                            )

//                            state = State.STATE_INITIALIZED
                            Log.d(
                                TAG,
                                "PLAYLIST_UPDATE: 3.$TAG, onChildrenLoaded(). Данные загружены, кладём их в mediaItemsListLiveData"
                            )
                        }
                    }
                })
        } catch (e2: IOException) {
            e2.printStackTrace()
            _errorMessageLiveData.postValue(
                Event(
                    Resource.error(
                        "Something went wrong while loading country stations playlist",
                        null
                    )
                )
            )
        }
    }

//    fun skipToNextSong() {
//        musicServiceConnection.transportControls.skipToNext()
//    }
//
//    fun skipToPreviousSong() {
//        musicServiceConnection.transportControls.skipToPrevious()
//    }
//
//    fun seekTo(pos: Long) {
//        musicServiceConnection.transportControls.seekTo(pos)
//    }

    fun stateInitialized() {
        state = State.STATE_INITIALIZED
    }

    // isPrepared, isPlaying, isPlayEnabled <- it's our extensions
    // In our case, METADATA_KEY_MEDIA_ID = radioStationRemote.url
    fun playOrToggleSong(mediaItem: RadioStationPresentation, toggle: Boolean = false) {

        viewModelScope.launch(Dispatchers.IO) {
            try {
                synchronized(mediaItem) {
                    // Checking by our Extensions from playbackState. If it is not prepared - false
                    val isPrepared = playbackStateLiveData.value?.isPrepared ?: false

                    // if we want to play the same song (pause and play it again)
                    // curPlayingSong.value?.getString(METADATA_KEY_MEDIA_ID) <- it's how we get metadata of currently playing song
                    if (isPrepared &&
                        mediaItem.stationuuid ==
                        curPlayingSongLiveData.value?.getString(METADATA_KEY_MEDIA_ID)
                    ) {
                        Log.d(
                            TAG,
                            "Включаем/выключаем ту же самую песню ${mediaItem.stationName}, url = ${mediaItem.urlResolved}"
                        )

                        playbackStateLiveData.value?.let { playbackState ->
                            when {
                                playbackState.isPlaying -> {
//                                    // Если у нас загружен список Польских радиостанций и мы слушаем станцию, которую добавили в избранное, то в случае, если мы откроем список избранного, плейлист не обновится (т.к. станция играет та же самая)
//                                    // В таком случае получится, что в FirebaseMusicSource список избранного, а в уведомлении - список польских радиостанций. В таком случае если мы нажмем в уведомлении кнопку "след." получим ошибку, т.к. будет запрошено описание станции у FirebaseMusicSource, а у FirebaseMusicSource уже другой плейлист и это станции там нет
//                                    // Поэтому на всякий случай будем заново включать станцию, даже если выбрали ту же самую, если она добавлена в изранное
//                                    if (mediaItem.isStationInFavourite || mediaItem.countryCode.endsWith(
//                                            "_FAV"
//                                        )
//                                    ) {
//                                        musicServiceConnection.transportControls.playFromMediaId(
////                                        mediaItem.urlResolved,
//                                            mediaItem.stationuuid,
//                                            null
//                                        )
//                                        if (toggle) musicServiceConnection.transportControls.pause()
//                                        _switchViewPagerOnceAgainLiveData.postValue(mediaItem)
//                                    }
                                    // ^ check, if it's needed after adding a download button

                                    // Проверила. Нужно, но попробую другое условие:
                                    val isCurCountryCodeFAV =
                                        curPlayingSongLiveData.value?.description?.subtitle.toString()
                                            .endsWith("_FAV", true)
                                    val isToggleCountryCodeFAV =
                                        mediaItem.countryCode.endsWith("_FAV", true)

                                    if (isCurCountryCodeFAV != isToggleCountryCodeFAV) {
                                        Log.d(TAG, "Станция одна и та же, но одна из них не из избранного. Cтанция: ${mediaItem.stationName}, код страны: ${mediaItem.countryCode}")
                                        musicServiceConnection.transportControls.playFromMediaId(
//                                        mediaItem.urlResolved,
                                            mediaItem.stationuuid,
                                            null
                                        )
                                        if (toggle) musicServiceConnection.transportControls.pause()
                                        _switchViewPagerOnceAgainLiveData.postValue(mediaItem)
                                    }

                                    if (toggle) musicServiceConnection.transportControls.pause()
                                }

                                playbackState.isPlayEnabled -> {
                                    // Создадим уведомление (Snackbar.make)
//                                    _messageLiveData.postValue(AUDIO_CONNECTING) -> вместо этого у нас полоса прогресса на экране
                                    musicServiceConnection.transportControls.play()
                                }

                                else -> Unit
                            }

                            saveLastUsedRadioStationUrlAndCode(
                                mediaItem.urlResolved,
                                mediaItem.countryCode
                            )
                            _switchViewPagerOnceAgainLiveData.postValue(mediaItem)
                            hideProgressAndSetClickable()
                        }

                        // if we want to play another song
                    } else {
                        // Создадим уведомление (Snackbar.make)
//                      _messageLiveData.postValue(AUDIO_CONNECTING) -> вместо этого у нас полоса прогресса на экране
                        Log.d(TAG, "Включаем другую песню ${mediaItem.stationName}, url = ${mediaItem.urlResolved}")

                        musicServiceConnection.transportControls.playFromMediaId(
                            mediaItem.stationuuid,
//                        mediaItem.urlResolved,
                            null
                        )
                        saveLastUsedRadioStationUrlAndCode(
                            mediaItem.urlResolved,
                            mediaItem.countryCode
                        )
                        _switchViewPagerOnceAgainLiveData.postValue(mediaItem)
                    }

                }
            } catch (e1: AccountsException) {
                // AccountsException -> Known direct subclasses: AuthenticatorException, NetworkErrorException, OperationCanceledException
                e1.printStackTrace()
                _dialogInternetTroubleLiveData.call()
            } catch (e2: IOException) {
                e2.printStackTrace()
                _errorMessageLiveData.postValue(
                    Event(
                        Resource.error(
                            "Something went wrong while interacting with a radio station",
                            null
                        )
                    )
                )
            }
        }

    }

    private fun saveLastUsedRadioStationUrlAndCode(urlResolved: String, countryCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                mainRadioInteractor.saveLastUsedRadioStationUrlAndCode(urlResolved, countryCode)
                _dataSavedSuccessfulLiveData.call()
            } catch (e1: AccountsException) {
                // AccountsException -> Known direct subclasses: AuthenticatorException, NetworkErrorException, OperationCanceledException
                e1.printStackTrace()
                _dialogInternetTroubleLiveData.call()
            } catch (e2: IOException) {
                e2.printStackTrace()
                _errorMessageLiveData.postValue(
                    Event(
                        Resource.error(
                            "Failed to store the last used radio station URL in the local database",
                            null
                        )
                    )
                )
            }
        }
    }

    // To change a playlist
    fun fetchSongs(countryCode: String) {
        val args = Bundle()
        args.putString("nRecNo", countryCode)
        musicServiceConnection.sendCommand(ADD_SONGS, args)

//        _setNonClickableLiveData.call()
    }

    fun showProgressAndDisableClick(stringDpOrCRSt: String) {
        when (stringDpOrCRSt) {
            "Dp" -> _setNonClickableDpLiveData.call()
            "CRSt" -> _setNonClickableCRStLiveData.call()
            else -> Log.d(TAG, "Unknown String in showProgressAndDisableClick()")
        }
        Log.d(TAG, "BROADCAST: Показываем прогресс, вызван метод showProgressAndDisableClick()")
    }

    fun hideProgressAndSetClickable() {
        _setClickableLiveData.call()
        Log.d(TAG, "BROADCAST: Прячем прогресс, вызван метод hideProgressAndSetClickable()")
    }

    fun saveNewMediaId(mediaId: String) {
        _newMediaIdLiveData.postValue(mediaId)
    }

    // Test version Нужно вызывать метод playOrToggleSong, когда у нас новый плейлист а песня была на паузе. И в то же время не нужно autoplay сразу при запуске программы. Поставим флажок
    fun notJustLaunchedEnableAutoplay() {
        _isNotJustLaunchedLiveData.postValue(true)
    }

    fun checkIsStationInFavouritesAndChangeTheStar(currentRadioStation: RadioStationPresentation) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Проверяем, есть ли станция в избранном
                if (currentRadioStation.isStationInFavourite) {
                    // Если станция есть в избранном и нажали на звезду, нужно из избранного удалить и убрать звезду
                    // Меняем isStationInFavourite = false в Room для последующих обращений к БД
                    homeRadioInteractor.deleteStationInRoomFromFavourite(currentRadioStation)
                    _stationDeletedFromFavouritesLiveData.call()
                } else {
                    // Если станции в избранном нет, нужно добавить её в избранное и поставить звезду
                    // Меняем isStationInFavourite = true в Room для последующих обращений к БД
                    homeRadioInteractor.addStationInRoomToFavourites(currentRadioStation)
                    _stationSavedInFavouritesLiveData.call()
                }
            } catch (e1: AccountsException) {
                // AccountsException -> Known direct subclasses: AuthenticatorException, NetworkErrorException, OperationCanceledException
                e1.printStackTrace()
                _dialogInternetTroubleLiveData.call()
            } catch (e: IOException) {
                e.printStackTrace()
                _errorMessageLiveData.postValue(
                    Event(
                        Resource.error(
                            "Failed connecting to the local database",
                            null
                        )
                    )
                )
            }
        }
    }

    fun changeTheStar(isFavourite: Boolean) {
        _changeTheStarLiveData.postValue(isFavourite)
    }

    fun addAStationToFavouriteListIfItIsNotThere(radioStationFavourite: RadioStationPresentation) {
        _addAStationToFavouriteListIfItIsNotThereLiveData.postValue(radioStationFavourite)
    }

    fun dialogInternetTroubleCall() {
        _dialogInternetTroubleLiveData.call()
    }

    fun errorMessagePost(message: String) {
        _errorMessageLiveData.postValue(
            Event(
                Resource.error(
                    message,
                    null
                )
            )
        )
    }

    fun markRadioStationAsPopularSendGetRequest(stationUuid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            mainRadioInteractor.markRadioStationAsPopularSendGetRequest(stationUuid)
        }
    }

//    fun checkThePosition(position: Int, radioStationList: List<RadioStationPresentation>) {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                var newPosition = position
//                var radioStationNeedToFind: RadioStationPresentation? = null
//                val mediaId: String? = _newMediaIdLiveData.value
//
//                // For sure, calculating chosen position
//                if (radioStationList.isNotEmpty() && !mediaId.isNullOrBlank()) {
//                    radioStationList.forEach {
//                        if (it.urlResolved == mediaId) {
//                            radioStationNeedToFind = it
//                        }
//                    }
//                }
//
//                radioStationNeedToFind?.let {
//                    // looking for the index of that song
//                    val newItemIndex = radioStationList.indexOf(radioStationNeedToFind)
//                    // That function will return -1 if the song doesn't exist, so we must check:
//                    if (newItemIndex != -1) {
//                        if (newItemIndex >= radioStationList.size) {
//                            Log.d(
//                                TAG,
//                                "onPageSelected 2) checkThePosition -> radioStationList.indexOf(radioStationNeedToFind) >= radioStationList.size, found: $newItemIndex"
//                            )
//                        } else {
//                            Log.d(
//                                TAG,
//                                "onPageSelected 2) checkThePosition -> radioStationList.indexOf(radioStationNeedToFind) != -1, position found: newPosition = $newItemIndex"
//                            )
//                            newPosition = newItemIndex
//                        }
//                    } else {
//                        Log.d(
//                            TAG,
//                            "onPageSelected 2) checkThePosition -> radioStationList.indexOf(radioStationNeedToFind) = -1, found: $newItemIndex"
//                        )
//                    }
//                }
//
//                Log.d(
//                    TAG,
//                    "onPageSelected 3) _newPositionLiveData.postValue(newPosition), position given: $newPosition, station need to play: $radioStationNeedToFind"
//                )
//                _newPositionLiveData.postValue(newPosition)
//            } catch (e1: AccountsException) {
//                // AccountsException -> Known direct subclasses: AuthenticatorException, NetworkErrorException, OperationCanceledException
//                e1.printStackTrace()
//                _dialogInternetTroubleLiveData.call()
//            } catch (e2: IOException) {
//                e2.printStackTrace()
//                _errorMessageLiveData.postValue(
//                    Event(
//                        Resource.error(
//                            "An unknown error occurred",
//                            null
//                        )
//                    )
//                )
//            }
//        }
//    }

    @Synchronized
    fun synchronizedCheckThePosition(
        position: Int,
        radioStationListFromLiveData: List<RadioStationPresentation>?,
        radioStationListFromSwipeAdapterNonNullButCanBeEmpty: List<RadioStationPresentation>
    ) {
//        viewModelScope.launch(Dispatchers.Default) {

        try {

            var newPosition = position
            var radioStationList = radioStationListFromLiveData

            // Если наша liveData пуста, возьмем список, который есть у нас в swipe adapter
            if (radioStationListFromLiveData == null) {
                radioStationList = radioStationListFromSwipeAdapterNonNullButCanBeEmpty
            }

            var maxRadioStationListIndex = 0
            radioStationList?.let { maxRadioStationListIndex = it.size - 1 }

            // If position = 0, check the position
            // Если мы скачиваем новый плейлист, то здесь всегда сначала получаем position = 0
            // Нужно проверить, действительно ли мы выбрали первую песню в плейлисте
//            if (newPosition == 0) {
//                var radioStationNeedToFind: RadioStationPresentation? = null
//                val mediaId: String? = _newMediaIdLiveData.value
//
//                // For sure, calculating chosen position
//                radioStationList?.let { nonNullRadioStationList ->
//
//                    if (nonNullRadioStationList.isNotEmpty() && !mediaId.isNullOrBlank()) {
//                        // Иногда находит несколько радиостанций с одинаковым url. Остановимся на первой.
//
////                        val maxListIndex = nonNullRadioStationList.size - 1
//                        for (i in 0..maxRadioStationListIndex) {
//                            if (nonNullRadioStationList[i].stationuuid == mediaId) {
//                                radioStationNeedToFind = nonNullRadioStationList[i]
//                                newPosition = i
//                                Log.d(
//                                    TAG,
//                                    "position found: newPosition = $newPosition"
//                                )
//                                break
//                            }
//                        }
//                    }
//
//                }
//                // Т.обр., если мы нашли нужную радиостанцию в списке, radioStationNeedToFind != null. Тогда записываем нужную позицию.
//                // Если же не нашли - позиция остаётся то же, какая и прилетела в метод изначально
//
//                Log.d(
//                    TAG,
//                    "position found: $newPosition, station need to play: $radioStationNeedToFind"
//                )
//            }

            // Previous it was fun NAME POSITION:

            val playbackState = playbackStateLiveData.value

            // We must check, if player is playing
            if (playbackState?.isPlaying == true) {

                // Если выбрать радиостанцию US (2000 Rock ...), а после неё первое Белорусское радио в списке (альфарадио) - вылетает IndexOutOfBoundsException, т.к. сначала ищет 300+ индекс в списке из 53х
                try {
                    if (newPosition <= maxRadioStationListIndex) {
                        Log.d(TAG, "position <= maxIndex")

//                    playOrToggleSong(radioStationListFromSwipeAdapterNonNullButCanBeEmpty[newPosition])
                        radioStationList?.get(newPosition)?.let { playOrToggleSong(it) }
                    }
                } catch (e: IndexOutOfBoundsException) {
                    Log.d(TAG, "CAUGHT IndexOutOfBoundsException!")
                    e.printStackTrace()
                }

            } else {

                // При включении программы и загрузке контента так же попадаем сюда
                try {
                    if (newPosition <= maxRadioStationListIndex) {
                        Log.d(TAG, "position <= maxIndex")

                        _updateCurPlayingRadioStationLiveData.postValue(
                            radioStationList?.get(newPosition)
                        )
                    }

                    /** Нам нужно вернуться в onPrepareFromMediaId, если мы выбрали песню из другого плейлиста и включить её. НО! Нам не нужно включать станцию сразу при включении программы **/
                    val isNotJustLaunched = _isNotJustLaunchedLiveData.value
                    isNotJustLaunched?.let {
                        if (it) {
                            // Здесь мы точно перешли из списка в HomeRadioFragment и хотим включить радио
                            radioStationList?.let { list ->
                                if (list.isNotEmpty() && newPosition < list.size) {
                                    playOrToggleSong(
                                        list[newPosition],
                                        true
                                    )
                                } // Если список пуст, значит это список избранного, который не заполнен
                            }
                        }
                    }
                } catch (e: IndexOutOfBoundsException) {
                    Log.d(TAG, "fun namePosition - CACHED IndexOutOfBoundsException!")
                    e.printStackTrace()
                }
            }

        } catch (e1: AccountsException) {
            // AccountsException -> Known direct subclasses: AuthenticatorException, NetworkErrorException, OperationCanceledException
            e1.printStackTrace()
            _dialogInternetTroubleLiveData.call()
        } catch (e2: IOException) {
            e2.printStackTrace()
            _errorMessageLiveData.postValue(
                Event(
                    Resource.error(
                        "An unknown error occurred",
                        null
                    )
                )
            )
        }

//        }
    }

//    fun mediaMetadataCompatToRadioStationPresentation(curPlayingRadioStation: MediaMetadataCompat?): RadioStationPresentation? {
//        try {
//            viewModelScope.launch(Dispatchers.IO) {
//                val curPlayingRadioStation =
//                    mainRadioInteractor.mediaMetadataCompatToRadioStationPresentation(
//                        curPlayingRadioStation
//                    )
//                )
//            }
//        } catch (e2: IOException) {
//            e2.printStackTrace()
//            _failedLiveData.call() // TODO use Resource class and its message
//        }
//    }

//    fun findRadioStationByMediaId(
//        radioStationList: List<RadioStationPresentation>,
//        mediaId: String
//    ) {
//        viewModelScope.launch(Dispatchers.Default) {
//            try {
//
//                var radioStationNeedToFind: RadioStationPresentation? = null
//
//                radioStationList.forEach {
//                    if (it.url == mediaId) {
//                        radioStationNeedToFind = it
//                    }
//                }
//
//                radioStationNeedToFind?.let {
//                    _radioStationNeedToFindLiveData.postValue(it)
//                }
//
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//        }
//    }

    // проверка, подключен ли интернет
    fun isInternetAvailable(context: Context): Boolean {
        var result = false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            result = when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            connectivityManager.run {
                connectivityManager.activeNetworkInfo?.run {
                    result = when (type) {
                        ConnectivityManager.TYPE_WIFI -> true
                        ConnectivityManager.TYPE_MOBILE -> true
                        ConnectivityManager.TYPE_ETHERNET -> true
                        else -> false
                    }

                }
            }
        }

        return result
    }

    // when View model is destroyed - заканчиваем нашу связь с сервисом
    override fun onCleared() {
        musicServiceConnection.unsubscribe(
            MEDIA_ROOT_ID,
            object : MediaBrowserCompat.SubscriptionCallback() {})

        super.onCleared()
    }

}
