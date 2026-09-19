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
import com.myproject.radiojourney.utils.exoplayer.ReadinessState
import com.myproject.radiojourney.utils.exoplayer.State.STATE_INITIALIZED
import com.myproject.radiojourney.utils.exoplayer.State.STATE_INITIALIZING
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
        val CANCEL_PLAYLIST_DOWNLOAD_COMMAND =
            SessionCommand(CANCEL_PLAYLIST_DOWNLOAD, Bundle.EMPTY)
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
            readiness.state = STATE_INITIALIZED
        }
        downloadJob = null
    }

    // Состояние загрузки плейлиста по команде ADD_SONGS и ожидающие её действия (общий класс, см. ReadinessState)
    private val readiness = ReadinessState()

    // Разрешаем подключившимся (экрану приложения, уведомлению) наши команды
    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val sessionCommands =
            MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
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
                Log.d(
                    TAG,
                    "PLAYLIST_UPDATE: Загрузка плейлиста $downloadingCountryCode отменена по таймауту"
                )
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
            Log.d(
                TAG,
                "PLAYLIST_UPDATE: Плейлист countryCode = $countryCode уже скачивается, повторную загрузку не запускаем"
            )
            return
        }

        // Если ещё скачивается ДРУГОЙ плейлист - отменяем его. Иначе старая загрузка могла закончиться позже новой
        // и перезаписать плейлист, или показать ошибку "получен пустой список", которая относится уже не к этому запросу
        cancelPlaylistDownload()
        downloadingCountryCode = countryCode

        // Загрузка выполняется прямо в downloadJob: при отмене downloadJob отменяется и она.
        // Раньше здесь были вложенные launch + join и catch (IOException), который не мог сработать: загрузка не бросает IOException
        downloadJob = serviceScope.launch {
            readiness.state = STATE_INITIALIZING

            // Чтобы проверить, может быть такой плейлист уже скачан и сейчас используется, обновим переменную
            firebaseMusicSource.radioStations.firstOrNull()?.let {
                lastCountryCode = it.mediaMetadata.subtitle.toString()
            }

            when {
                countryCode == "FAV" && lastCountryCode.orEmpty().endsWith("_FAV") ->
                    Log.d(
                        TAG,
                        "!! PLAYLIST_UPDATE: 5. FAV_STAR: Список избранного уже скачан в exoplayer"
                    )

                countryCode == "FAV" -> {
                    Log.d(
                        TAG,
                        "PLAYLIST_UPDATE: 5. FAV_STAR: Скачиваем список избранного в exoplayer"
                    )
                    firebaseMusicSource.fetchFavouriteMediaData()
                }

                lastCountryCode != countryCode || lastCountryCode?.endsWith(
                    "_FAV",
                    true
                ) == true -> {
                    Log.d(
                        TAG,
                        "PLAYLIST_UPDATE: 2.$TAG, addSongs(). Скачиваем плейлист, т.к. $lastCountryCode != $countryCode"
                    )
                    firebaseMusicSource.fetchMediaData(
                        if (!countryCode.isNullOrBlank() && countryCode != "null") countryCode else DEFAULT_COUNTRY_CODE
                    )
                }

                else -> Log.d(
                    TAG,
                    "PLAYLIST_UPDATE: Список countryCode = $countryCode уже скачан в exoplayer"
                )
            }

            // Плейлист готов (или загрузка не удалась, а текущий плейлист остался рабочим): ожидающие получают ответ.
            // При пустом избранном ожидающий выбор станции получит "станции нет в плейлисте" и не будет ждать бесконечно
            readiness.state = STATE_INITIALIZED
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
            return Futures.immediateFuture(
                MediaItemsWithStartPosition(
                    mediaItems,
                    startIndex,
                    startPositionMs
                )
            )
        }

        val mediaId = requestedItem.mediaId
        val result = SettableFuture.create<MediaItemsWithStartPosition>()

        // true - станция найдена в текущем плейлисте, результат отправлен
        fun completeIfFound(): Boolean {
            val radioStations = firebaseMusicSource.radioStations
            val index = radioStations.indexOfFirst { it.mediaId == mediaId }
            if (index == -1) return false
            lastCountryCode =
                radioStations[index].mediaMetadata.subtitle.toString() // Обновляем переменную класса после поиска
            Log.d(
                TAG,
                "PLAYLIST_UPDATE: 2.$TAG, onSetMediaItems(). Включаем станцию ${radioStations[index].mediaMetadata.title}"
            )
            result.set(MediaItemsWithStartPosition(radioStations, index, C.TIME_UNSET))
            return true
        }

        firebaseMusicSource.whenReady {
            serviceScope.launch(Dispatchers.Main) {
                if (completeIfFound()) return@launch
                // Станции нет в плейлисте: возможно, как раз скачивается новый плейлист - ждём его
                readiness.whenReady {
                    serviceScope.launch(Dispatchers.Main) {
                        if (!completeIfFound()) {
                            Log.d(
                                TAG,
                                "PLAYLIST_UPDATE: 2.$TAG, onSetMediaItems(). Станция $mediaId не найдена в плейлисте"
                            )
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
    ): ListenableFuture<LibraryResult<MediaItem>> =
        Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))

    // Нужен для подписки на список станций (MediaBrowser.subscribe): по умолчанию сессия проверяет, что корень - папка
    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String
    ): ListenableFuture<LibraryResult<MediaItem>> {
        if (mediaId == MEDIA_ROOT_ID) return Futures.immediateFuture(
            LibraryResult.ofItem(
                rootItem,
                null
            )
        )
        val item = firebaseMusicSource.radioStations.find { it.mediaId == mediaId }
        return Futures.immediateFuture(
            if (item != null) LibraryResult.ofItem(item, null) else LibraryResult.ofError(
                SessionError.ERROR_BAD_VALUE
            )
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
                    val radioStations = firebaseMusicSource.radioStations
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
        val lastIndex =
            radioStations.indexOfFirst { it.localConfiguration?.uri.toString() == lastUsedRadioStationUrl }
        player.setMediaItems(radioStations, lastIndex.coerceAtLeast(0), C.TIME_UNSET)
        Log.d(
            TAG,
            "PLAYLIST_UPDATE: 5.$TAG, плейлист положен в плейер, станция ${lastIndex.coerceAtLeast(0)}"
        )
    }
}
