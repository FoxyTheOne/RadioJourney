package com.myproject.radiojourney.presentation.mainActivity

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import com.myproject.radiojourney.other.Constants.MEDIA_ROOT_ID
import com.myproject.radiojourney.other.Resource
import com.myproject.radiojourney.utils.exoplayer.MusicServiceConnection
import com.myproject.radiojourney.utils.extension.isPlayEnabled
import com.myproject.radiojourney.utils.extension.isPlaying
import com.myproject.radiojourney.utils.extension.isPrepared
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val musicServiceConnection: MusicServiceConnection
) : ViewModel() {
    // LiveData contains the media data for our activity
    private val _mediaItems = MutableLiveData<Resource<List<RadioStationPresentation>>>()
    val mediaItems: LiveData<Resource<List<RadioStationPresentation>>> = _mediaItems

    // LiveData from our ServiceConnection
    val isConnected = musicServiceConnection.isConnected
    val networkError = musicServiceConnection.networkError
    val curPlayingSong = musicServiceConnection.curPlayingSong
    val playbackState = musicServiceConnection.playbackState


    init {
        // Here we start query media items, so let's put it into LiveData:
        _mediaItems.postValue(Resource.loading(null)) // Resource data loading status. Null as default - we don't have any data here yet. Т.е. мы кладём в _mediaItems LiveData значение - объект класса Resource с нужным нам флагом и данными
        musicServiceConnection.subscribe(MEDIA_ROOT_ID, object : MediaBrowserCompat.SubscriptionCallback() {
            override fun onChildrenLoaded(
                parentId: String,
                children: MutableList<MediaBrowserCompat.MediaItem>
            ) {
                super.onChildrenLoaded(parentId, children)
                // And here we convert children: MutableList<MediaBrowserCompat.MediaItem> to our format of data
                // Данные подтягиваются из FirebaseMusicSource.asMediaItems(), .fetchMediaData()
                val items = children.map {
                    RadioStationPresentation(
                        stationName = it.description.title.toString(),
                        url = it.mediaId!!,
                        urlResolved = it.description.mediaUri.toString(),
                        clickCount = 1, // TODO
                        countryCode = it.description.description.toString(),
                        isStationInFavourite = false, // TODO
                        isStationInRecommended = false // TODO
                    )
                }
                _mediaItems.postValue(Resource.success(items))
            }
        })
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
        val isPrepared = playbackState.value?.isPrepared ?: false // Checking by our Extensions from playbackState. If it is not prepared - false
        // if we want to play the same song (pause and play it again)
        if(isPrepared && mediaItem.url ==
            curPlayingSong.value?.getString(METADATA_KEY_MEDIA_ID)) { // curPlayingSong.value?.getString(METADATA_KEY_MEDIA_ID) <- it's how we get metadata of currently playing song
            playbackState.value?.let { playbackState ->
                when {
                    playbackState.isPlaying -> if(toggle) musicServiceConnection.transportControls.pause()
                    playbackState.isPlayEnabled -> musicServiceConnection.transportControls.play()
                    else -> Unit
                }
            }
            // if we want to play another song
        } else {
            musicServiceConnection.transportControls.playFromMediaId(mediaItem.url, null)
        }
    }

    // when View model is destroyed - заканчиваем нашу связь с сервисом
    override fun onCleared() {
        super.onCleared()
        musicServiceConnection.unsubscribe(MEDIA_ROOT_ID, object : MediaBrowserCompat.SubscriptionCallback() {})
    }
}
