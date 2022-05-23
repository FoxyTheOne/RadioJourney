package com.myproject.radiojourney.utils.exoplayer

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.*
import androidx.core.net.toUri
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.myproject.radiojourney.data.dataSource.network.INetworkRadioDataSource
import com.myproject.radiojourney.utils.exoplayer.State.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

// TODO RENAME

// We need time to upload music from firebase or other data
class FirebaseMusicSource @Inject constructor(
    private val networkRadioDataSource: INetworkRadioDataSource
) {
    // 2.
    var radioStations = emptyList<MediaMetadataCompat>() // meta info about radioStations

    suspend fun fetchMediaData() = withContext(Dispatchers.IO) {
        state = STATE_INITIALIZING
        val allRadioStations = networkRadioDataSource.getAllRadioStationsList()

        // TODO
        radioStations = allRadioStations.map { radioStationRemote ->
            MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_ARTIST, radioStationRemote.country)
                .putString(METADATA_KEY_MEDIA_ID, radioStationRemote.url)
                .putString(METADATA_KEY_TITLE, radioStationRemote.name)
                .putString(METADATA_KEY_DISPLAY_TITLE, radioStationRemote.name)
                .putString(METADATA_KEY_MEDIA_URI, radioStationRemote.url_resolved)
                .putString(METADATA_KEY_DISPLAY_SUBTITLE, radioStationRemote.country)
                .putString(METADATA_KEY_DISPLAY_DESCRIPTION, radioStationRemote.countrycode)
                .putLong(METADATA_KEY_USER_RATING, radioStationRemote.clickcount.toLong())
                .build()
        }
        state = STATE_INITIALIZED
    }

    // Для формирования плейлиста из нескольких песен/радиостанций. Info for exoplayer to stream songs
    // TODO составлять список в плейлист из одной, выбранной страныю После того, как переделаем список с сервера в MAP
    fun asMediaSource(dataSourceFactory: DefaultDataSourceFactory): ConcatenatingMediaSource {
        val concatenatingMediaSource = ConcatenatingMediaSource() // empty by default
        radioStations.forEach { radioStation ->
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(radioStation.getString(METADATA_KEY_MEDIA_URI).toUri())
            concatenatingMediaSource.addMediaSource(mediaSource) // Add one by one to our concatenatingMediaSource
        }
        return concatenatingMediaSource
    }

    // A list of media items
    fun asMediaItems() = radioStations.map { radioStation ->
        val desc = MediaDescriptionCompat.Builder()
            .setTitle(radioStation.description.title)
            .setMediaId(radioStation.description.mediaId)
            .setMediaUri(radioStation.getString(METADATA_KEY_MEDIA_URI).toUri())
            .setSubtitle(radioStation.description.subtitle)
            .build()
        MediaBrowserCompat.MediaItem(desc, FLAG_PLAYABLE)
    }.toMutableList()

    //1.
    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    private var state: State = STATE_CREATED // State on default
        set(value) {
            if(value == STATE_INITIALIZED || value == STATE_ERROR) {
                synchronized(onReadyListeners) { // synchronized for save change
                    field = value // sign a new value to the field
                    onReadyListeners.forEach { listener ->
                        listener(state == STATE_INITIALIZED) // go through list and call needed lambda function. If there will be STATE_ERROR instead STATE_INITIALIZED, we will get "false". So we can check, if it was successful or not
                    }
                }
            } else {
                field = value // if it is STATE_CREATED or STATE_INITIALIZING
            }
        }

    // A function which will add actions to our list of actions (returns boolean - if it is ready or not)
    fun whenReady(action: (Boolean) -> Unit): Boolean {
        if(state == STATE_CREATED || state == STATE_INITIALIZING) {
            onReadyListeners += action // We are not ready, so just add action to list (we will do it later, when we will be ready)
            return false // not ready
        } else {
            action(state == STATE_INITIALIZED) // we are ready, so we can call action
            return true
        }
    }
}

enum class State {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}