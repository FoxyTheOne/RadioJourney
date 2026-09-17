package com.myproject.radiojourney.utils.exoplayer.callback

import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.myproject.radiojourney.other.Constants.ADD_SONGS
import com.myproject.radiojourney.other.Constants.CANCEL_PLAYLIST_DOWNLOAD
import com.myproject.radiojourney.other.Constants.COUNTRY_CODE_ID
import com.myproject.radiojourney.other.Constants.DEFAULT_COUNTRY_CODE
import com.myproject.radiojourney.other.Constants.MEDIA_ROOT_ID
import com.myproject.radiojourney.other.Constants.NETWORK_ERROR
import com.myproject.radiojourney.utils.exoplayer.FirebaseMusicSource
import com.myproject.radiojourney.utils.exoplayer.callback.State.STATE_CREATED
import com.myproject.radiojourney.utils.exoplayer.callback.State.STATE_ERROR
import com.myproject.radiojourney.utils.exoplayer.callback.State.STATE_INITIALIZED
import com.myproject.radiojourney.utils.exoplayer.callback.State.STATE_INITIALIZING
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Колбэк медиасессии media3 (MediaLibrarySession). Заменяет MusicPlaybackPreparer (MediaSessionConnector.PlaybackPreparer)
 * и onGetRoot / onLoadChildren из MediaBrowserServiceCompat:
 * - onCustomCommand: команды экрана "загрузить плейлист" (ADD_SONGS) и "отменить загрузку" (CANCEL_PLAYLIST_DOWNLOAD) - раньше onCommand;
 * - onSetMediaItems: включить станцию по mediaId - раньше onPrepareFromMediaId;
 * - onGetLibraryRoot / onGetItem / onGetChildren: список станций для экрана - раньше onGetRoot / onLoadChildren.
 *
 * Все методы вызываются в главном потоке.
 */
@OptIn(UnstableApi::class) // AcceptedResultBuilder, DEFAULT_SESSION_AND_LIBRARY_COMMANDS, onSetMediaItems, MediaItemsWithStartPosition (см. ServiceModule)
class MusicLibrarySessionCallback(
    private val firebaseMusicSource: FirebaseMusicSource,
    private val serviceScope: CoroutineScope,
    private val player: Player,
    private val getLastUsedRadioStationUrl: () -> String,
    private val onNetworkError: () -> Unit
) : MediaLibrarySession.Callback {

    companion object {
        private const val TAG = "MusicSessionCallback"

        val ADD_SONGS_COMMAND = SessionCommand(ADD_SONGS, Bundle.EMPTY)
        val CANCEL_PLAYLIST_DOWNLOAD_COMMAND = SessionCommand(CANCEL_PLAYLIST_DOWNLOAD, Bundle.EMPTY)
        val NETWORK_ERROR_COMMAND = SessionCommand(NETWORK_ERROR, Bundle.EMPTY)
    }

    private var lastCountryCode: String? = null

    // Код страны плейлиста, который сейчас скачивается (защита от двух одновременных загрузок одного и того же плейлиста)
    private var downloadingCountryCode: String? = null

    // Текущая загрузка плейлиста (команда ADD_SONGS), чтобы её можно было отменить
    private var downloadJob: Job? = null

    // Корень "библиотеки": папка со станциями текущего плейлиста
    private val rootItem = MediaItem.Builder()
        .setMediaId(MEDIA_ROOT_ID)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS)
                .build()
        )
        .build()

    // Отменяет загрузку плейлиста, если она идёт
    private fun cancelPlaylistDownload() {
        if (downloadJob?.isActive == true) {
            downloadJob?.cancel() // вложенные загрузки (launch внутри) отменятся вместе с ней
            downloadingCountryCode = null
            // Текущий плейлист остаётся рабочим - разблокируем лямбды whenReady()
            state = STATE_INITIALIZED
        }
        downloadJob = null
    }

    // Список лямбд action, которые будут передаваться в метод whenReady(), пока state == STATE_CREATED или state == STATE_INITIALIZING
    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    // Параметр state с setter для того, чтобы можно было привязать к этому параметру определенную логику
    private var state: State = STATE_CREATED // State on default
        set(value) {
            if (value == STATE_INITIALIZED || value == STATE_ERROR) {
                synchronized(onReadyListeners) { // synchronized for save change
                    field = value // sign a new value to the field
                    // Каждая лямбда должна сработать один раз, иначе при следующей загрузке плейлиста снова сработает старая
                    val listeners = onReadyListeners.toList()
                    onReadyListeners.clear()
                    listeners.forEach { listener ->
                        listener(state == STATE_INITIALIZED) // If there will be STATE_ERROR instead STATE_INITIALIZED, we will get "false"
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

    // Разрешаем подключившимся (экрану приложения, уведомлению) наши команды
    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
            .add(ADD_SONGS_COMMAND)
            .add(CANCEL_PLAYLIST_DOWNLOAD_COMMAND)
            .build()
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(sessionCommands)
            .build()
    }

    // Раньше - MusicPlaybackPreparer.onCommand()
    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
            //edit data or fetch more data from api
            ADD_SONGS -> addSongs(args.getString(COUNTRY_CODE_ID))

            // Полоса загрузки висела слишком долго (MainViewModel, PROGRESS_TIMEOUT): отменяем загрузку,
            // чтобы её результат (например, ошибка) не появился позже, когда пользователь уже делает что-то другое
            CANCEL_PLAYLIST_DOWNLOAD -> {
                Log.d(TAG, "PLAYLIST_UPDATE: Загрузка плейлиста $downloadingCountryCode отменена по таймауту")
                cancelPlaylistDownload()
            }

            else -> return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    private fun addSongs(countryCode: String?) {
        // fetchSongs() вызывается два раза подряд (RadioListFragment и HomeRadioFragment). Если этот плейлист уже скачивается,
        // вторую загрузку не запускаем, иначе две параллельные загрузки дважды перезаписывают плейлист
        if (countryCode != null && countryCode == downloadingCountryCode) {
            Log.d(TAG, "PLAYLIST_UPDATE: Плейлист countryCode = $countryCode уже скачивается, повторную загрузку не запускаем")
            return
        }

        // Если ещё скачивается ДРУГОЙ плейлист - отменяем его. Иначе старая загрузка могла закончиться позже новой
        // и перезаписать плейлист, или показать ошибку "получен пустой список", которая относится уже не к этому запросу
        cancelPlaylistDownload()
        downloadingCountryCode = countryCode

        downloadJob = serviceScope.launch {
            state = STATE_INITIALIZING

            // Чтобы проверить, может быть такой плейлист уже скачан и сейчас используется, обновим переменную
            if (firebaseMusicSource.radioStations.isNotEmpty()) {
                lastCountryCode = firebaseMusicSource.radioStations[0].mediaMetadata.subtitle.toString()
            }

            if (countryCode == "FAV") {
                val job = launch { // дочерняя корутина downloadJob - отменяется вместе с ней
                    try {
                        Log.d(TAG, "PLAYLIST_UPDATE: Проверяем список в exoplayer, lastCountryCode = $lastCountryCode")
                        if (!lastCountryCode.toString().endsWith("_FAV")) {
                            // Скачиваем список избранного
                            firebaseMusicSource.fetchFavouriteMediaData()
                            Log.d(TAG, "PLAYLIST_UPDATE: 5. FAV_STAR: Запускаем метод для скачивания списка избранного в exoplayer")
                        } else {
                            Log.d(TAG, "!! PLAYLIST_UPDATE: 5. FAV_STAR: Список избранного уже скачан в exoplayer")
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Log.d(TAG, "!! PLAYLIST_UPDATE: IOException !!")
                    }
                }
                job.join()
                if (!firebaseMusicSource.isFavoriteEmpty) state = STATE_INITIALIZED

            } else if (lastCountryCode != countryCode || lastCountryCode?.endsWith("_FAV", true) == true) {
                Log.d(TAG, "PLAYLIST_UPDATE: 2.$TAG, addSongs(). Скачиваем плейлист, т.к. $lastCountryCode != $countryCode")
                val job = launch { // дочерняя корутина downloadJob - отменяется вместе с ней
                    try {
                        firebaseMusicSource.fetchMediaData(
                            if (countryCode.toString() != "null" && countryCode.toString().isNotBlank()) countryCode.toString()
                            else DEFAULT_COUNTRY_CODE
                        )
                    } catch (e: IOException) {
                        // Когда сохранён не верный CountryCode, по запросу такого не найдёт и выдаст ошибку retrofit2.HttpException: HTTP 404
                        Log.d(TAG, "PLAYLIST_UPDATE: 2.$TAG, addSongs(). Не получилось скачать плейлист. Exception: ${e.message}")
                        e.printStackTrace()
                        firebaseMusicSource.fetchMediaData(DEFAULT_COUNTRY_CODE)
                    }
                }
                job.join()
                Log.d(TAG, "PLAYLIST_UPDATE: 2.$TAG, addSongs(). Дождались окончания загрузки нового плейлиста")
                state = STATE_INITIALIZED

            } else {
                Log.d(TAG, "PLAYLIST_UPDATE: Список countryCode = $countryCode уже скачан в exoplayer")
                // Плейлист уже скачан - значит всё готово. Иначе state навсегда остаётся STATE_INITIALIZING
                state = STATE_INITIALIZED
            }

            downloadingCountryCode = null
        }
    }

    // Включить станцию по mediaId. Экран вызывает MediaBrowser.setMediaItem(MediaItem с одним mediaId) - раньше playFromMediaId().
    // Сессия заменяет этот MediaItem на весь текущий плейлист и начинает с нужной станции.
    // Если плейлист ещё скачивается - ждём окончания загрузки
    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<MediaItemsWithStartPosition> {
        val requestedItem = mediaItems.singleOrNull()
        if (requestedItem == null || requestedItem.localConfiguration != null) {
            // Пришли готовые станции с адресами - ставим как есть
            return Futures.immediateFuture(MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs))
        }

        val mediaId = requestedItem.mediaId
        val result = SettableFuture.create<MediaItemsWithStartPosition>()

        // true - станция найдена в текущем плейлисте, результат отправлен
        fun completeIfFound(): Boolean {
            val radioStations = firebaseMusicSource.radioStations
            val index = radioStations.indexOfFirst { it.mediaId == mediaId }
            if (index == -1) return false
            lastCountryCode = radioStations[index].mediaMetadata.subtitle.toString() // Обновляем переменную класса после поиска
            Log.d(TAG, "PLAYLIST_UPDATE: 2.$TAG, onSetMediaItems(). Включаем станцию ${radioStations[index].mediaMetadata.title}")
            result.set(MediaItemsWithStartPosition(radioStations, index, C.TIME_UNSET))
            return true
        }

        firebaseMusicSource.whenReady {
            serviceScope.launch(Dispatchers.Main) {
                if (completeIfFound()) return@launch
                // Станции нет в плейлисте: возможно, как раз скачивается новый плейлист - ждём его
                whenReady {
                    serviceScope.launch(Dispatchers.Main) {
                        if (!completeIfFound()) {
                            Log.d(TAG, "PLAYLIST_UPDATE: 2.$TAG, onSetMediaItems(). Станция $mediaId не найдена в плейлисте")
                            result.setException(IllegalStateException("Radio station $mediaId is not in the playlist"))
                        }
                    }
                }
            }
        }
        return result
    }

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> = Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))

    // Нужен для подписки на список станций (MediaBrowser.subscribe): по умолчанию сессия проверяет, что корень - папка
    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String
    ): ListenableFuture<LibraryResult<MediaItem>> {
        if (mediaId == MEDIA_ROOT_ID) return Futures.immediateFuture(LibraryResult.ofItem(rootItem, null))
        val item = firebaseMusicSource.radioStations.find { it.mediaId == mediaId }
        return Futures.immediateFuture(
            if (item != null) LibraryResult.ofItem(item, null) else LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
        )
    }

    // Список станций текущего плейлиста для экрана. Раньше - onLoadChildren(MEDIA_ROOT_ID)
    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        if (parentId != MEDIA_ROOT_ID) {
            return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
        }

        val result = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
        firebaseMusicSource.whenReady { isInitialized ->
            serviceScope.launch(Dispatchers.Main) {
                if (isInitialized) {
                    val radioStations = firebaseMusicSource.asMediaItems()
                    setInitialPlaylistIfEmpty(radioStations)
                    result.set(LibraryResult.ofItemList(radioStations, params))
                } else {
                    // Сеть недоступна - сообщаем экрану (MusicServiceConnection -> networkErrorLiveData)
                    onNetworkError()
                    result.set(LibraryResult.ofError(SessionError.ERROR_IO))
                }
            }
        }
        return result
    }

    // При первом запуске кладём плейлист в плейер (без prepare()), чтобы в плеере на экране сразу была станция,
    // которую слушали в прошлый раз. prepare() не вызываем: иначе плеер сразу начнёт загружать поток
    // и media3 покажет уведомление, хотя радио не включали
    private fun setInitialPlaylistIfEmpty(radioStations: List<MediaItem>) {
        if (player.mediaItemCount > 0 || radioStations.isEmpty()) return
        val lastUsedRadioStationUrl = getLastUsedRadioStationUrl()
        val lastIndex = radioStations.indexOfFirst { it.localConfiguration?.uri.toString() == lastUsedRadioStationUrl }
        player.setMediaItems(radioStations, lastIndex.coerceAtLeast(0), C.TIME_UNSET)
        Log.d(TAG, "PLAYLIST_UPDATE: 5.$TAG, плейлист положен в плейер, станция ${lastIndex.coerceAtLeast(0)}")
    }
}

enum class State {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}
