package com.myproject.radiojourney.presentation

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.mainRadioUseCase.IMainRadioUseCase
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import com.myproject.radiojourney.other.Constants.ADD_SONGS
import com.myproject.radiojourney.other.Constants.MEDIA_ROOT_ID
import com.myproject.radiojourney.other.Resource
import com.myproject.radiojourney.utils.exoplayer.MusicServiceConnection
import com.myproject.radiojourney.utils.extension.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val musicServiceConnection: MusicServiceConnection,
    private val mainRadioInteractor: IMainRadioUseCase
) : ViewModel() {
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

    // LiveData from our ServiceConnection
    val isConnectedLiveData = musicServiceConnection.isConnectedLiveData
    val networkErrorLiveData = musicServiceConnection.networkErrorLiveData
    val playbackStateLiveData = musicServiceConnection.playbackStateLiveData
    val curPlayingSongLiveData = musicServiceConnection.curPlayingSongLiveData

    // If smth went wrong
    private val _failedLiveData = MutableLiveData<Boolean>()
    val failedLiveData: MutableLiveData<Boolean> = _failedLiveData

    init {
        try {
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

                        }
                    }
                })
        } catch (e2: IOException) {
            e2.printStackTrace()
            _failedLiveData.call() // TODO use Resource class and its message
        }
    }

    fun skipToNextSong() {
        musicServiceConnection.transportControls.skipToNext()
    }

    fun skipToPreviousSong() {
        musicServiceConnection.transportControls.skipToPrevious()
    }

    fun seekTo(pos: Long) {
        musicServiceConnection.transportControls.seekTo(pos)
    }

    // isPrepared, isPlaying, isPlayEnabled <- it's our extensions
    // In our case, METADATA_KEY_MEDIA_ID = radioStationRemote.url
    fun playOrToggleSong(mediaItem: RadioStationPresentation, toggle: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isPrepared = playbackStateLiveData.value?.isPrepared
                    ?: false // Checking by our Extensions from playbackState. If it is not prepared - false

                // if we want to play the same song (pause and play it again)
                if (isPrepared && mediaItem.url ==
                    curPlayingSongLiveData.value?.getString(METADATA_KEY_MEDIA_ID)
                ) { // curPlayingSong.value?.getString(METADATA_KEY_MEDIA_ID) <- it's how we get metadata of currently playing song
                    playbackStateLiveData.value?.let { playbackState ->
                        when {
                            playbackState.isPlaying -> if (toggle) musicServiceConnection.transportControls.pause()
                            playbackState.isPlayEnabled -> musicServiceConnection.transportControls.play()
                            else -> Unit
                        }
                        saveLastUsedRadioStationUrlAndCode(mediaItem.url, mediaItem.countryCode)
                    }

                    // if we want to play another song
                } else {
                    musicServiceConnection.transportControls.playFromMediaId(mediaItem.url, null)
                    saveLastUsedRadioStationUrlAndCode(mediaItem.url, mediaItem.countryCode)
                }
            } catch (e2: IOException) {
                e2.printStackTrace()
                _failedLiveData.call() // TODO use Resource class and its message
            }
        }
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

    private fun saveLastUsedRadioStationUrlAndCode(url: String, countryCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                mainRadioInteractor.saveLastUsedRadioStationUrlAndCode(url, countryCode)
                _dataSavedSuccessfulLiveData.call()
            } catch (e2: IOException) {
                e2.printStackTrace()
                _failedLiveData.call() // TODO use Resource class and its message
            }
        }
    }

    // !!! Попробуем изменять плейлист
    fun fetchSongs(countryCode: String) {
        val args = Bundle()
//        args.putInt("nRecNo", 2)
        args.putString("nRecNo", countryCode)
        musicServiceConnection.sendCommand(ADD_SONGS, args)
    }

    // when View model is destroyed - заканчиваем нашу связь с сервисом
    override fun onCleared() {
        super.onCleared()
        musicServiceConnection.unsubscribe(
            MEDIA_ROOT_ID,
            object : MediaBrowserCompat.SubscriptionCallback() {})
    }

    fun checkThePosition(position: Int, radioStationList: List<RadioStationPresentation>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var newPosition = position
                var radioStationNeedToFind: RadioStationPresentation? = null
                val mediaId: String? = newMediaIdLiveData.value

                // For sure, calculating chosen position
                if (radioStationList.isNotEmpty() && !mediaId.isNullOrBlank()) {
                    radioStationList.forEach {
                        if (it.url == mediaId) {
                            radioStationNeedToFind = it
                        }
                    }
                }

                radioStationNeedToFind?.let {
                    // looking for the index of that song
                    val newItemIndex = radioStationList.indexOf(radioStationNeedToFind)
                    // That function will return -1 if the song doesn't exist, so we must check:
                    if (newItemIndex != -1) newPosition = newItemIndex
                }

                _newPositionLiveData.postValue(newPosition)
            } catch (e2: IOException) {
                e2.printStackTrace()
                _failedLiveData.call() // TODO use Resource class and its message
            }
        }
    }

    fun saveNewMediaId(mediaId: String) {
        _newMediaIdLiveData.postValue(mediaId)
    }

    // TODO Test version Нужно вызывать метод playOrToggleSong, когда у нас новый плейлист а песня была на паузе. И в то же время не нужно autoplay стразу при запуске программы. Поставим флажок
    fun notJustLaunchedEnableAutoplay() {
        _isNotJustLaunchedLiveData.postValue(true)
    }
}
