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
    // Список, куда будут сохраняться метаданные по каждой радиостанции с помощью метода fetchMediaData()
    var radioStations = emptyList<MediaMetadataCompat>() // meta info about radioStations

    // Список лямбд action, которые будут передаваться в метод whenReady(), пока state == STATE_CREATED или state == STATE_INITIALIZING
    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    // Параметр state с setter для того, чтобы можно было привязать к этому параметру определенную логику
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
        return if(state == STATE_CREATED || state == STATE_INITIALIZING) {
            onReadyListeners += action // We are not ready, so just add action to list (we will do it later, when we will be ready)
            false // not ready
        } else {
            action(state == STATE_INITIALIZED) // we are ready, so we can call action
            true
        }
    }

    // Метод для СОХРАНЕНИЯ МЕТАДАННЫХ по каждой радиостанции. Создаём список MediaMetadataCompat
    suspend fun fetchMediaData() = withContext(Dispatchers.IO) {
        state = STATE_INITIALIZING
        val allRadioStations = networkRadioDataSource.getAllRadioStationsList()

        // TODO огромный ответ, долго ждать
        radioStations = allRadioStations.map { radioStationRemote ->
            MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_MEDIA_ID, radioStationRemote.url) // media Id / url (Primary key)
                .putString(METADATA_KEY_MEDIA_URI, radioStationRemote.url_resolved) // url_resolved
                .putString(METADATA_KEY_TITLE, radioStationRemote.name) // station name
                .putString(METADATA_KEY_DISPLAY_TITLE, radioStationRemote.name) // station name
                .putLong(METADATA_KEY_DOWNLOAD_STATUS, radioStationRemote.clickcount.toLong()) // click count
                .putString(METADATA_KEY_ARTIST, radioStationRemote.countrycode) // country code ?? (instead of country)
                .putString(METADATA_KEY_DISPLAY_SUBTITLE, radioStationRemote.country) // country
//                .putString(METADATA_KEY_ALBUM_ARTIST, radioStationRemote.countrycode) // country code
                .build()
        }
        state = STATE_INITIALIZED
    }

    // A list of media items. Список MediaMetadataCompat теперь преобразуем в список MediaBrowserCompat.MediaItem (для нашей MainViewModel). Сформированный список вернется как результат работы функции там, где её вызвали.
    // Метод необходимо выхывать после того, как список radioStations будет полностью сформирован!
    fun asMediaItems() = radioStations.map { radioStation ->
        val extrasRadioStationInfo = Bundle().apply {
            putLong("ClickCount", radioStation.getLong(METADATA_KEY_DOWNLOAD_STATUS))
            putString("CountryCode", radioStation.getString(METADATA_KEY_ARTIST))
        }

        val desc = MediaDescriptionCompat.Builder()
            .setMediaId(radioStation.description.mediaId) // media Id / url (Primary key)
            .setMediaUri(radioStation.getString(METADATA_KEY_MEDIA_URI).toUri()) // url_resolved
            .setTitle(radioStation.description.title) // station name
            .setSubtitle(radioStation.description.subtitle) // country
            .setExtras(extrasRadioStationInfo) // <- click count, country code in extras
            .build()
        MediaBrowserCompat.MediaItem(desc, FLAG_PLAYABLE)
    }.toMutableList() // Flag FLAG_PLAYABLE indicates that the item is playable, not the item that has children of its own.

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
}

enum class State {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}