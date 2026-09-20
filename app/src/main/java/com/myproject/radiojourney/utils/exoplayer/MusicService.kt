package com.myproject.radiojourney.utils.exoplayer

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.myproject.radiojourney.R
import com.myproject.radiojourney.domain.iRepository.IMainRadioStationRepository
import com.myproject.radiojourney.other.Constants.DEFAULT_COUNTRY_CODE
import com.myproject.radiojourney.other.Constants.FAVOURITES_COUNTRY_CODE_SUFFIX
import com.myproject.radiojourney.other.Constants.MEDIA_ROOT_ID
import com.myproject.radiojourney.other.Constants.MY_STATIONS_COUNTRY_CODE
import com.myproject.radiojourney.other.Constants.NOTIFICATION_CHANNEL_ID
import com.myproject.radiojourney.other.Constants.NOTIFICATION_ID
import com.myproject.radiojourney.other.Constants.PAUSED_NOTIFICATION_TIMEOUT
import com.myproject.radiojourney.utils.exoplayer.callback.MusicLibrarySessionCallback
import com.myproject.radiojourney.utils.exoplayer.callback.MusicPlayerEventListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Сервис с плеером (media3).
 * MusicService наследуется от MediaLibraryService (media3) - раньше MediaBrowserServiceCompat.
 * Медиасессия (MediaLibrarySession) соединяет плеер с экраном приложения (MusicServiceConnection), уведомлением,
 * наушниками и т.п. Уведомление и foreground-режим сервиса media3 ведёт сама (раньше - PlayerNotificationManager
 * и MusicPlayerNotificationListener).
 * Создаём CoroutineScope для задач, решаемых в сервисе, чтобы не перегружать наш main thread (don't forget serviceScope.cancel() in onDestroy!)
 * Fetching our metadata from our class, created earlier (radioPlaylistSource)
 */
// @OptIn, а не @UnstableApi: @UnstableApi на классе требовал бы такой же пометки везде, где упоминается MusicService
@OptIn(UnstableApi::class) // setForegroundServiceTimeoutMs, setShowNotificationForIdlePlayer, isPlaybackOngoing, DefaultMediaNotificationProvider.setSmallIcon
@AndroidEntryPoint
class MusicService : MediaLibraryService() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

    @Inject
    lateinit var radioPlaylistSource: RadioPlaylistSource

    @Inject
    lateinit var mainRadioStationRepository: IMainRadioStationRepository

    // Create a coroutine scope to avoid using main thread for our tasks
    private val serviceJob = Job()

    // Непойманное исключение в корутине сервиса (например, некорректный ответ сервера при загрузке плейлиста)
    // раньше роняло всё приложение. Теперь записываем его в лог, а плеер продолжает работать с текущим плейлистом
    private val serviceExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Uncaught exception in MusicService coroutine", throwable)
    }
    private val serviceScope =
        CoroutineScope(Dispatchers.Main + serviceJob + serviceExceptionHandler)

    private lateinit var mediaLibrarySession: MediaLibrarySession

    private lateinit var musicPlayerEventListener: MusicPlayerEventListener

    // Уведомление на паузе. media3 показывает уведомление, пока плеер подготовлен (не STATE_IDLE), и после паузы держит
    // сервис в foreground не дольше 10 минут - потом уведомление остаётся уже без foreground-сервиса.
    // Xiaomi (MIUI) в таком состоянии убивает процесс при смахивании приложения, и уведомление остаётся "мёртвым".
    // Поэтому через PAUSED_NOTIFICATION_TIMEOUT на паузе останавливаем плеер (STATE_IDLE) - media3 убирает уведомление
    // (setShowNotificationForIdlePlayer NEVER). Foreground держим чуть дольше этого таймаута
    private var pauseTimeoutJob: Job? = null
    private val pauseTimeoutListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (!events.containsAny(
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_PLAY_WHEN_READY_CHANGED
                )
            ) return
            if (isPlayingOrStarting()) {
                pauseTimeoutJob?.cancel()
                pauseTimeoutJob = null
            } else if (player.playbackState != Player.STATE_IDLE && pauseTimeoutJob == null) {
                pauseTimeoutJob = serviceScope.launch {
                    delay(PAUSED_NOTIFICATION_TIMEOUT)
                    Log.d(
                        TAG,
                        "Радио на паузе дольше $PAUSED_NOTIFICATION_TIMEOUT мс - останавливаем плеер и убираем уведомление"
                    )
                    pauseTimeoutJob = null
                    exoPlayer.stop() // освобождаем поток. Нажатие play подготовит станцию заново
                }
            }
        }
    }

    companion object {
        private const val TAG = "MusicService"
    }

    override fun onCreate() {
        super.onCreate()

        // Загружаем плейлист, который слушали в прошлый раз (при первом запуске - DEFAULT_COUNTRY_CODE).
        // Ошибки загрузки обрабатывает RadioPlaylistSource, а непредвиденные исключения - serviceExceptionHandler
        // (раньше здесь были catch SocketTimeoutException / IOException, которые не могли сработать)
        serviceScope.launch {
            val lastPlayedCountryCode =
                mainRadioStationRepository.getLastUsedRadioStationCountryCode()
            Log.d(TAG, "Узнаём последний используемый код страны - $lastPlayedCountryCode")

            when {
                lastPlayedCountryCode.endsWith(FAVOURITES_COUNTRY_CODE_SUFFIX, true) ->
                    radioPlaylistSource.fetchFavouriteMediaData()

                // "MY" - это не страна, а плейлист своих станций. Без этой ветки при следующем запуске
                // приложение приняло бы код за Малайзию и включило бы её станции вместо добавленных пользователем
                lastPlayedCountryCode == MY_STATIONS_COUNTRY_CODE -> {
                    radioPlaylistSource.fetchMyStationsMediaData()
                    // Пользователь удалил все свои станции - плейлист оказался бы пустым, и плеер внизу экрана
                    // остался бы без станций. В этом случае показываем плейлист по умолчанию
                    if (radioPlaylistSource.radioStations.isEmpty()) {
                        radioPlaylistSource.fetchMediaData(DEFAULT_COUNTRY_CODE)
                    }
                }

                else -> radioPlaylistSource.fetchMediaData(
                    lastPlayedCountryCode.takeIf { it.isNotBlank() && it != "null" }
                        ?: DEFAULT_COUNTRY_CODE
                )
            }
        }

        // Pending intent for opening our activity when we click on notification
        val openActivityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val sessionCallback = MusicLibrarySessionCallback(
            radioPlaylistSource = radioPlaylistSource,
            serviceScope = serviceScope,
            player = exoPlayer,
            getLastUsedRadioStationUrl = { mainRadioStationRepository.getLastUsedRadioStationUrl() },
            onNetworkError = {
                // Where we will set the NETWORK_ERROR, so that we can catch it in MusicServiceConnection
                mediaLibrarySession.broadcastCustomCommand(
                    MusicLibrarySessionCallback.NETWORK_ERROR_COMMAND,
                    Bundle.EMPTY
                )
            }
        )

        mediaLibrarySession = MediaLibrarySession.Builder(this, exoPlayer, sessionCallback)
            .apply { openActivityIntent?.let { setSessionActivity(it) } }
            .build()

        // Уведомление плеера: наш канал, id и значок. Название станции, страна и картинка берутся из MediaMetadata станции
        // (RadioPlaylistSource.toRadioStationMediaItem)
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(NOTIFICATION_CHANNEL_ID)
                .setChannelName(R.string.notification_channel_name)
                .setNotificationId(NOTIFICATION_ID)
                .build()
                .apply { setSmallIcon(R.drawable.ic_music_note_orange) }
        )
        // Не показывать уведомление, пока плеер не подготовлен (радио ещё не включали или его остановили по таймауту паузы)
        setShowNotificationForIdlePlayer(MediaSessionService.SHOW_NOTIFICATION_FOR_IDLE_PLAYER_NEVER)
        // На паузе держим сервис в foreground, пока не сработает наш таймаут (см. pauseTimeoutListener)
        setForegroundServiceTimeoutMs(PAUSED_NOTIFICATION_TIMEOUT + 10_000L)

        // Скачан новый плейлист - экран (MusicServiceConnection) запросит список станций заново.
        // Раньше - LiveData с observeForever, которую нужно было не забыть отписать в onDestroy; корутина serviceScope отменится сама
        serviceScope.launch {
            radioPlaylistSource.playlistChanged.collect {
                mediaLibrarySession.notifyChildrenChanged(
                    MEDIA_ROOT_ID,
                    radioPlaylistSource.radioStations.size,
                    null
                )
            }
        }

        // Прогресс загрузки плейлиста и ошибку сервера экран получает напрямую из PlaylistDownloadStatus (раньше сервис пересылал их бродкастами)

        musicPlayerEventListener = MusicPlayerEventListener(this)
        exoPlayer.addListener(musicPlayerEventListener)
        exoPlayer.addListener(pauseTimeoutListener)

    }

    // Запущенный сервис будет работать пока у него не вызван stopSelf().
    // Передавать данные в сервис можно так же с помощью startService(intent),
    // новый сервис запускаться при этом не будет, а у запущенного сервиса будет вызван onStartCommand.

    private fun isPlayingOrStarting(): Boolean =
        exoPlayer.playWhenReady &&
                (exoPlayer.playbackState == Player.STATE_BUFFERING || exoPlayer.playbackState == Player.STATE_READY)

    // Сессия для подключающихся контроллеров (экран приложения, уведомление, наушники)
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        mediaLibrarySession

    // Приложение закрыли (смахнули из недавних).
    // Радио играет - продолжаем играть, уведомление остаётся (на паузе оно уберётся само через PAUSED_NOTIFICATION_TIMEOUT).
    // Радио не играет - останавливаем плеер (media3 убирает уведомление) и завершаем сервис
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (isPlaybackOngoing() && isPlayingOrStarting()) return
        exoPlayer.stop()
        pauseAllPlayersAndStopSelf()
    }

    override fun onDestroy() {
        Log.d(TAG, "MUSIC SERVICE IS DESTROYED -> вызван метод onDestroy()")

        serviceScope.cancel()

        exoPlayer.removeListener(musicPlayerEventListener)
        exoPlayer.removeListener(pauseTimeoutListener)
        mediaLibrarySession.release()
        exoPlayer.release()

        super.onDestroy()
    }

}