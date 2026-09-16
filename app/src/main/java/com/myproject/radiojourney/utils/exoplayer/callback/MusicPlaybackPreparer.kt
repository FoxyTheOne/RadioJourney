package com.myproject.radiojourney.utils.exoplayer.callback

import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.myproject.radiojourney.other.Constants
import com.myproject.radiojourney.other.Constants.COUNTRY_CODE_ID
import com.myproject.radiojourney.other.Constants.DEFAULT_COUNTRY_CODE
import com.myproject.radiojourney.other.Constants.CANCEL_PLAYLIST_DOWNLOAD
import com.myproject.radiojourney.other.Constants.PLAYLIST_ID
import com.myproject.radiojourney.utils.exoplayer.FirebaseMusicSource
import com.myproject.radiojourney.utils.exoplayer.callback.State.STATE_CREATED
import com.myproject.radiojourney.utils.exoplayer.callback.State.STATE_ERROR
import com.myproject.radiojourney.utils.exoplayer.callback.State.STATE_INITIALIZED
import com.myproject.radiojourney.utils.exoplayer.callback.State.STATE_INITIALIZING
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException

class MusicPlaybackPreparer(
    private val firebaseMusicSource: FirebaseMusicSource,
    private val serviceScope: CoroutineScope,
    private val onPrepareRequested: () -> Unit, // 006 claude // подготовить плейер заново (он остановлен, но плейлист в нём остался)
    private val playerPrepared: (MediaMetadataCompat?) -> Unit // lambda, that can be called when our player is prepared
) : MediaSessionConnector.PlaybackPreparer {

    companion object {
        private const val TAG = "MusicPlaybackPreparer"
    }

    private var lastCountryCode: String? = null

    // <!-- 005 claude
    // Код страны плейлиста, который сейчас скачивается (защита от двух одновременных загрузок одного и того же плейлиста)
    private var downloadingCountryCode: String? = null

    // Текущая загрузка плейлиста (команда "Add Songs"), чтобы её можно было отменить
    private var downloadJob: Job? = null

    // Отменяет загрузку плейлиста, если она идёт. Вызывается в главном потоке (onCommand)
    private fun cancelPlaylistDownload() {
        if (downloadJob?.isActive == true) {
            downloadJob?.cancel() // вложенные загрузки (launch внутри) отменятся вместе с ней
            downloadingCountryCode = null
            // Текущий плейлист остаётся рабочим - разблокируем лямбды whenReady()
            state = STATE_INITIALIZED
        }
        downloadJob = null
    }
    // 005 claude -->

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
                    // <!--001 Claude
//                    onReadyListeners.forEach { listener ->
// Каждая лямбда должна сработать один раз, иначе при следующей загрузке плейлиста снова вызовется playerPrepared() со старым mediaId
                    val listeners = onReadyListeners.toList()
                    onReadyListeners.clear()
                    listeners.forEach { listener ->
                        // 001 Claude -->
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

            // Edit data or fetch more data from api
            "Add Songs" -> {

                // <!-- 005 claude
//                serviceScope.launch {

//                    state = STATE_INITIALIZING

                    // Достаём country code и далее сравниваем его. Если коды разные, скачиваем новый плейлист
                    val countryCode = extras?.getString(COUNTRY_CODE_ID)

//                    <!-- 001 claude
                    // fetchSongs() вызывается два раза подряд (RadioListFragment и HomeRadioFragment). Если этот плейлист уже скачивается,
                    // вторую загрузку не запускаем, иначе две параллельные загрузки дважды перезаписывают плейлист
                    if (countryCode != null && countryCode == downloadingCountryCode) {
                        Log.d(
                            TAG,
                            "PLAYLIST_UPDATE: Плейлист countryCode = $countryCode уже скачивается, повторную загрузку не запускаем"
                        )
//                        return@launch
                        return false
                    }


                // Если ещё скачивается ДРУГОЙ плейлист - отменяем его. Иначе старая загрузка могла закончиться позже новой
                // и перезаписать плейлист, или показать ошибку "получен пустой список", которая относится уже не к этому запросу
                cancelPlaylistDownload()

                    downloadingCountryCode = countryCode
//                    001 claude -->

                downloadJob = serviceScope.launch {
//                    005 claude -->

                    state = STATE_INITIALIZING
                    if (countryCode == "FAV") {

//                        <!-- 005 claude
//                        val job = serviceScope.launch {
                            val job = launch { // дочерняя корутина downloadJob - отменяется вместе с ней
//                          005 claude -->

                                try {
                                // Чтобы проверить, может быть такой плейлист уже скачан и сейчас используется, обновим переменную
                                if (firebaseMusicSource.radioStations.isNotEmpty()) {
                                    lastCountryCode =
                                        firebaseMusicSource.radioStations[0].description.subtitle.toString()
                                }
                                Log.d(
                                    TAG,
                                    "PLAYLIST_UPDATE: Проверяем список в exoplayer, lastCountryCode = $lastCountryCode"
                                )

                                val isLastCountryCodeFavorite =
                                    lastCountryCode.toString().endsWith("_FAV")
                                Log.d(
                                    TAG,
                                    "PLAYLIST_UPDATE: 5. FAV_STAR: Проверяем список в exoplayer, isLastCountryCodeFavorite = $isLastCountryCodeFavorite"
                                )

                                if (!isLastCountryCodeFavorite) {
                                    // Скачиваем список избранного
                                    firebaseMusicSource.fetchFavouriteMediaData()
                                    Log.d(
                                        TAG,
                                        "PLAYLIST_UPDATE: 5. FAV_STAR: Запускаем метод для скачивания списка избранного в exoplayer"
                                    )
                                } else {
                                    Log.d(
                                        TAG,
                                        "!! PLAYLIST_UPDATE: 5. FAV_STAR: Список избранного уже скачан в exoplayer"
                                    )
                                }
                            } catch (e: IOException) {
                                e.printStackTrace()
                                // TODO Fill error message to LiveData, но я не знаю, куда эти данные передавать - нет класса, который следит за этим классом
                                Log.d(TAG, "!! PLAYLIST_UPDATE: IOException !!")
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

                        if (lastCountryCode != countryCode || lastCountryCode?.endsWith(
                                "_FAV",
                                true
                            ) == true
                        ) {
                            Log.d(
                                TAG,
                                "PLAYLIST_UPDATE: 2.$TAG, onCommand(). Запускаем метод для скачивания списка радиостанций в exoplayer, т.к. $lastCountryCode != $countryCode . Если $countryCode пуст или равен нулю, будет скачиваться список по коду AD"
                            )
//                            if (countryCode.toString().endsWith("_anyway")) {
//                                val str: String = countryCode.toString()
//                                val n = 7 // "_anyway" -> 7 chars
//
//                                val newCountryCode = str.removeLastNchars(str,n)
//
//                                countryCode = newCountryCode
//                            }

                            // <!-- 005 claude
//                            val job = serviceScope.launch {
                            val job = launch { // дочерняя корутина downloadJob - отменяется вместе с ней
                                // 005 claude -->

                                try {
                                    firebaseMusicSource.fetchMediaData(
                                        if (countryCode.toString() != "null" && countryCode.toString()
                                                .isNotBlank()
                                        ) countryCode.toString()
                                        else DEFAULT_COUNTRY_CODE
                                    )
                                } catch (e: IOException) {
                                    // Когда сохранён не верный CountryCode, по запросу такого не найдёт и выдаст ошибку retrofit2.HttpException: HTTP 404
                                    Log.d(
                                        TAG,
                                        "PLAYLIST_UPDATE: 2.$TAG, onCommand(). Не получилось скачать плейлист. Exception: ${e.message}. Problem occurred in method onCommand."
                                    )
                                    e.printStackTrace()
                                    firebaseMusicSource.fetchMediaData(DEFAULT_COUNTRY_CODE)
                                    Log.d(
                                        TAG,
                                        "PLAYLIST_UPDATE: 2.$TAG, onCommand(). Запускаем метод для скачивания списка радиостанций в exoplayer с кодом DEFAULT_COUNTRY_CODE"
                                    )
                                    // TODO Была такая ошибка из-за проблемы с интернетом. Сделать высвечивание сообщения об ошибке, чтобы понимали, почему скачался и включился не тот плейлист /  Сделала сообщение, проверить
                                }
                            }
                            job.join()
                            Log.d(
                                TAG,
                                "PLAYLIST_UPDATE: 2.$TAG, onCommand(). Дождались окончания загрузки нового плейлиста в exoplayer"
                            )
                            state = STATE_INITIALIZED
                        } else {
                            Log.d(
                                TAG,
                                "PLAYLIST_UPDATE: Список countryCode = $countryCode уже скачан в exoplayer"
                            )

//                            <!--001 claude
                            // Плейлист уже скачан - значит всё готово. Иначе state навсегда остаётся STATE_INITIALIZING (выставлен в начале команды),
                            // и лямбды из onPrepareFromMediaId, добавленные через whenReady(), не вызываются до следующей загрузки
                            state = STATE_INITIALIZED
//                            001 claude -->

                        }
                    }

                    downloadingCountryCode = null // 001 claude
                }

                // <!-- 005 claude
            }

            // Полоса загрузки висела слишком долго (MainViewModel, PROGRESS_TIMEOUT): отменяем загрузку,
            // чтобы её результат (например, ошибка) не появился позже, когда пользователь уже делает что-то другое
            CANCEL_PLAYLIST_DOWNLOAD -> {
                Log.d(TAG, "PLAYLIST_UPDATE: Загрузка плейлиста $downloadingCountryCode отменена по таймауту")
                cancelPlaylistDownload()
            // 005 claude -->

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

    // <!-- 006 claude
//    // we won't implement now
//    override fun onPrepare(playWhenReady: Boolean) = Unit

    // Вызывается, когда нажали play, а плейер остановлен (STATE_IDLE): после ошибки воспроизведения или stop().
    // Раньше метод был пустым, и кнопка play в уведомлении в таком состоянии ничего не делала.
    // play() MediaSessionConnector вызовет сам после этого метода
    override fun onPrepare(playWhenReady: Boolean) {
        Log.d(TAG, "onPrepare(): плейер остановлен, готовим его заново")
        onPrepareRequested()
    }
    // 006 claude -->

    // function for preparing song that user selected
    override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle?) {
        // here we will need our function with stated from enum class (STATE_CREATED, STATE_INITIALIZING, STATE_INITIALIZED, STATE_ERROR)
        firebaseMusicSource.whenReady {
            // looking for a song with media id

//            val test = mediaId
//            val testRadioStations = firebaseMusicSource.radioStations

            var itemToPlay =
                firebaseMusicSource.radioStations.find { mediaId == it.description.mediaId }

            // Если выбираем другую страну, в этом месте он не находит радиостанцию и в MusicService (val musicPlaybackPreparer = MusicPlaybackPreparer(firebaseMusicSource, serviceScope) {...}) отправляет null

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

                        Log.d(
                            TAG,
                            "PLAYLIST_UPDATE: 2.$TAG, onPrepareFromMediaId(). !!! Вызываем playerPrepared(), itemToPlay = $itemToPlay, из лямбды whenReady"
                        )
                        playerPrepared(itemToPlay) // Метод иногда не вызывается (см. ниже). Пока что продублировала
                    }
                }
            }

            itemToPlay?.let {
                lastCountryCode =
                    it.description.subtitle.toString() // Обновляем переменную класса после поиска
            }

            Log.d(
                TAG,
                "PLAYLIST_UPDATE: 2.$TAG, onPrepareFromMediaId(). !!! Вызываем playerPrepared(), itemToPlay = $itemToPlay"
            )
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
