package com.myproject.radiojourney.utils.exoplayer

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.myproject.radiojourney.other.Constants.NETWORK_ERROR
import com.myproject.radiojourney.other.Event
import com.myproject.radiojourney.other.Resource

/**
 * A class for connection between activity or fragment with MusicService
 */
class MusicServiceConnection(context: Context) {
    // LiveData for our Service, where we will keep data (data for our fragments to update if server changes)
    private val _isConnectedLiveData =
        MutableLiveData<Event<Resource<Boolean>>>() // For current state. Event and Resource - are our classes
    val isConnectedLiveData: LiveData<Event<Resource<Boolean>>> = _isConnectedLiveData

    private val _networkErrorLiveData =
        MutableLiveData<Event<Resource<Boolean>>>() // It must be private, so that other classes can't change it
    val networkErrorLiveData: LiveData<Event<Resource<Boolean>>> =
        _networkErrorLiveData // And another LiveData, that equals to previous, so that classes can't change it

    private val _playbackStateLiveData =
        MutableLiveData<PlaybackStateCompat?>() // Is player playing or not
    val playbackStateLiveData: LiveData<PlaybackStateCompat?> = _playbackStateLiveData

    private val _curPlayingSongLiveData =
        MutableLiveData<MediaMetadataCompat?>() // Contains meta information of the song that is currently playing
    val curPlayingSongLiveData: LiveData<MediaMetadataCompat?> = _curPlayingSongLiveData

    lateinit var mediaController: MediaControllerCompat // 1. To use transport controls (pause, play the song, skip to the next) 2. For watching callbacks, that are useful for us here

    // To have access to token we also must create a mediaBrowser instance and for that we need this MediaBrowserConnectionCallback()
    // So, let's create an instance of mediaBrowserConnectionCallback()
    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)

    // And then, in the end - an instance of mediaBrowser
    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(
            context,
            MusicService::class.java
        ),
        mediaBrowserConnectionCallback,
        null
    ).apply { connect() } // ! Trigger a function to connect
    // now we can return to the function onConnected() in the inner class MediaBrowserConnectionCallback()

    // transportControls: pause, play the song, skip to the next etc.
    val transportControls: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls // transportControls are not initialized yet (!lateinit! var mediaController) so we need to use "get". Otherwise there will be a crash. Now it will be initialized only when we'll try to get access to it

    // Also let's create functions for subscribing and unsubscribing (for calling from view model):
    fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.subscribe(parentId, callback)
    }

    fun unsubscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.unsubscribe(parentId, callback)
    }

    // !!! Попробуем изменять плейлист
    fun sendCommand(command: String, parameters: Bundle?) =
        sendCommand(command, parameters) { _, _ -> }

    // !!! Попробуем изменять плейлист
    private fun sendCommand(
        command: String,
        parameters: Bundle?,
        resultCallback: ((Int, Bundle?) -> Unit)
    ) = if (mediaBrowser.isConnected) {
        mediaController.sendCommand(
            command,
            parameters,
            object : ResultReceiver(Handler(Looper.myLooper()!!)) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    resultCallback(resultCode, resultData)
                }
            })
        true
    } else {
        false
    }

    private inner class MediaBrowserConnectionCallback(
        private val context: Context
    ) : MediaBrowserCompat.ConnectionCallback() {

        // Once this musicService connection here is active, this function will be called
        override fun onConnected() {
            // Once it is connected, we have access to our session token and we can now initialize mediaController
            // But to have access to token we also must create a mediaBrowser instance and for that we need this MediaBrowserConnectionCallback(). So, return here later, when we will have that instance
            // After we have mediaBrowser instance, we can return to this method
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaContollerCallback()) // <- our second inner class
            }
            _isConnectedLiveData.postValue(Event(Resource.success(true))) // post connection data to LiveData
        }

        override fun onConnectionSuspended() {
            _isConnectedLiveData.postValue(
                Event(
                    Resource.error(
                        "The connection was suspended", false
                    )
                )
            )
        }

        override fun onConnectionFailed() {
            _isConnectedLiveData.postValue(
                Event(
                    Resource.error(
                        "Couldn't connect to media browser", false
                    )
                )
            )
        }
    }

    private inner class MediaContollerCallback : MediaControllerCompat.Callback() {
        // When playback state changes this function will be called
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            _playbackStateLiveData.postValue(state) // We are posting our state and now we have an access to it from our fragment
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            _curPlayingSongLiveData.postValue(metadata) // Getting new meta data (put it into LiveData)
        }

        // Send custom events from our service to this connection callback. We will use it to notify when there is a network error
        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            when (event) {
                // Ловим исключение в случае проблемы с сервером
                NETWORK_ERROR -> _networkErrorLiveData.postValue(
                    Event(
                        Resource.error(
                            "Couldn't connect to the server. Please check your internet connection.",
                            null
                        )
                    )
                )
                // Where we will set the NETWORK_ERROR, so that we can catch it here? We will do that in our MusicService
            }
        }

        // If our session is destroyed, we can call a function from mediaBrowserConnectionCallback() - so we will post an error status to our LiveData
        override fun onSessionDestroyed() {
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }
}