package com.myproject.radiojourney.utils.exoplayer.callback

import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.myproject.radiojourney.utils.exoplayer.FirebaseMusicSource
import com.myproject.radiojourney.utils.exoplayer.callback.State.*
import com.myproject.radiojourney.utils.extension.removeLastNchars
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException

class MusicPlaybackPreparer(
    private val firebaseMusicSource: FirebaseMusicSource,
    private val serviceScope: CoroutineScope,
    private val playerPrepared: (MediaMetadataCompat?) -> Unit // lambda, that can be called when our player is prepared
) : MediaSessionConnector.PlaybackPreparer {

    companion object {
        private const val TAG = "MusicPlaybackPreparer"
    }

    private var lastCountryCode: String? = null

    // Список лямбд action, которые будут передаваться в метод whenReady(), пока state == STATE_CREATED или state == STATE_INITIALIZING
//    private var onReadyListener : ((Boolean) -> Unit)? = null // нам нужна одна лямбда, самая последняя
    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    // Параметр state с setter для того, чтобы можно было привязать к этому параметру определенную логику
    private var state: State = STATE_CREATED // State on default
        set(value) {
            if (value == STATE_INITIALIZED || value == STATE_ERROR) {
                synchronized(onReadyListeners) { // synchronized for save change
                    field = value // sign a new value to the field
//                        it(state == STATE_INITIALIZED)
                    onReadyListeners.forEach { listener ->
                        listener(state == STATE_INITIALIZED) // go through list and call needed lambda function. If there will be STATE_ERROR instead STATE_INITIALIZED, we will get "false". So we can check, if it was successful or not
                    }
                }
            } else {
                field = value // if it is STATE_CREATED or STATE_INITIALIZING
            }
        }

    // A function which will add actions to our list of actions (returns boolean - if it is ready or not)
    private fun whenReady(action: (Boolean) -> Unit): Boolean {
        return if (state == STATE_CREATED || state == STATE_INITIALIZING) {
            onReadyListeners += action // We are not ready, so just add action to list (we will do it later, when we will be ready)
            false // not ready
        } else {
            action(state == STATE_INITIALIZED) // we are ready, so we can call action
            true
        }
    }

//    // we won't implement now
//    override fun onCommand(
//        player: Player,
//        controlDispatcher: ControlDispatcher,
//        command: String,
//        extras: Bundle?,
//        cb: android.os.ResultReceiver?
//    ): Boolean = false

    // !!! Попробуем изменять плейлист
    override fun onCommand(
        player: Player,
        command: String,
        extras: Bundle?,
        cb: android.os.ResultReceiver?
    ): Boolean {
        when (command) {

            //edit data or fetch more data from api
            "Add Songs" -> {

                serviceScope.launch {
                    state = STATE_INITIALIZING

                    var countryCode =
                        extras?.get("nRecNo") // Достаём country code и далее сравниваем его. Если коды разные, скачиваем новый плейлист

                    if (countryCode == "FAV") {

                        val job = serviceScope.launch {
                            try {
                                // Скачиваем список избранного
                                firebaseMusicSource.fetchFavouriteMediaData()
                            } catch (e: IOException) {
                                e.printStackTrace()
                                // TODO Fill error message to LiveData
                            }
                        }
                        job.join()
                        if (!firebaseMusicSource.isFavoriteEmpty) state = STATE_INITIALIZED

                    } else {

                        // Чтобы проверить, может быть такой плейлист уже скачан и сейчас используется, обновим переменную
                        if (firebaseMusicSource.radioStations.isNotEmpty()) {
                            lastCountryCode =
                                firebaseMusicSource.radioStations[0].description.subtitle.toString()
                        }

                        // if (lastCountryCode != null && lastCountryCode != countryCode) {
                        // Если оставлять lastCountryCode != null, сюда не заходит, если программу включили и выбрали станцию из другого плейлиста, не включая перед этим плейер ни разу
                        // Вместо этого проверим (выше), скачан ли уже такой плей лист и сравнивать будем с такой переменной:

                        if (lastCountryCode != countryCode) {
//                            if (countryCode.toString().endsWith("_anyway")) {
//                                val str: String = countryCode.toString()
//                                val n = 7 // "_anyway" -> 7 chars
//
//                                val newCountryCode = str.removeLastNchars(str,n)
//
//                                countryCode = newCountryCode
//                            }

                            val job = serviceScope.launch {
                                try {
                                    firebaseMusicSource.fetchMediaData(
                                        if (countryCode.toString() != "null" && countryCode.toString()
                                                .isNotBlank()
                                        ) countryCode.toString()
                                        else "AD"
                                    )
                                } catch (e: IOException) {
                                    // Когда сохранён не верный CountryCode, по запросу такого не найдёт и выдаст ошибку retrofit2.HttpException: HTTP 404
                                    e.printStackTrace()
                                    firebaseMusicSource.fetchMediaData("AD")
                                    // TODO Была такая ошибка из-за проблемы с интернетом. Сделать высвечивание сообщения об ошибке, чтобы понимали, почему скачался и включился не тот плейлист
                                }
                            }
                            job.join()
                            state = STATE_INITIALIZED
                        }
                    }

                }

            }

        }
        return false
    }

    // return a tab of actions that we support in our player
    override fun getSupportedPrepareActions(): Long {
        // we prepare a specific song with media id (that's why we added media id earlier) or play it
        // so, media id is used to select a specific song
        return PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
    }

    // we won't implement now
    override fun onPrepare(playWhenReady: Boolean) = Unit

    // function for preparing song that user selected
    override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle?) {
        // here we will need our function with stated from enum class (STATE_CREATED, STATE_INITIALIZING, STATE_INITIALIZED, STATE_ERROR)
        firebaseMusicSource.whenReady {
            // looking for a song with media id

            val test = mediaId
            val testRadioStations = firebaseMusicSource.radioStations

            var itemToPlay =
                firebaseMusicSource.radioStations.find { mediaId == it.description.mediaId }

            // Если выбираем другую страну, в этом месте он не находит радиостанцию и в MusicService (val musicPlaybackPreparer = MusicPlaybackPreparer(firebaseMusicSource, serviceScope) {...}) отправляет null

//            if (mediaId != "null" && itemToPlay == null) {
//                // Не знаю, как в этом место узнать код страны и скачать новый плейлист.
//                // Думаю, его нужно скачивать по клику на recycler view при переходе на фрагмент (вызывать метод из mainViewModel)
//                // TODO А здесь останавливаться до when ready (подсмотреть, как мы это делали). Когда плейлист скачался, ещё раз ищем itemToPlay
//
//                // TODO для начала попробовать просто вызывать fetch из вью модели по клику на элемент. Там (т.е. уже здесь, в onCommand) задать логику, если countru code совпадает, всё ок. Если нет - скачиваем
//
//                firebaseMusicSource.whenReady { isInitialized ->
//                    if (isInitialized) {
//                        itemToPlay =
//                            firebaseMusicSource.radioStations.find { mediaId == it.description.mediaId }
//                    }
//                }
//
//            }

//            while (mediaId != "null" && itemToPlay == null) {
//                itemToPlay =
//                    firebaseMusicSource.radioStations.find { mediaId == it.description.mediaId }
//            }

            // Задать if, чтобы лишний раз не заходило сюда
            if (mediaId != "null" && itemToPlay == null) {
                whenReady { isInitialized ->
                    if (isInitialized) {
                        itemToPlay =
                            firebaseMusicSource.radioStations.find { mediaId == it.description.mediaId }
                        lastCountryCode =
                            itemToPlay?.description?.subtitle.toString() // Обновляем переменную класса после поиска
                    }
                }
            }

//            lastCountryCode =
//                itemToPlay?.description?.subtitle.toString() // Обновляем переменную класса после поиска TODO let?. чтобы обновлять lastCountryCode на не null ????

            itemToPlay?.let {
                lastCountryCode =
                    it.description.subtitle.toString() // Обновляем переменную класса после поиска
            }

            playerPrepared(itemToPlay)
        }
    }

    // we won't implement now (from google voice, for instance)
    override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) = Unit

    // we won't implement now
    override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) = Unit
}

enum class State {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}
