package com.myproject.radiojourney.utils.exoplayer

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.myproject.radiojourney.R
import com.myproject.radiojourney.data.sharedPreference.IAppSharedPreference
import com.myproject.radiojourney.other.Constants
import com.myproject.radiojourney.other.Constants.DEFAULT_COUNTRY_CODE
import com.myproject.radiojourney.other.Constants.KEY_BROADCAST_COUNT_MA
import com.myproject.radiojourney.other.Constants.KEY_BROADCAST_LIST_SIZE_MA
import com.myproject.radiojourney.other.Constants.KEY_BROADCAST_SERVER_IS_DOWN
import com.myproject.radiojourney.other.Constants.MEDIA_ROOT_ID
import com.myproject.radiojourney.other.Constants.NOTIFICATION_CHANNEL_ID
import com.myproject.radiojourney.other.Constants.NOTIFICATION_ID
import com.myproject.radiojourney.other.Constants.PAUSED_NOTIFICATION_TIMEOUT
import com.myproject.radiojourney.utils.exoplayer.callback.MusicLibrarySessionCallback
import com.myproject.radiojourney.utils.exoplayer.callback.MusicPlayerEventListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject

/**
 * Сервис с плеером (media3).
 * MusicService наследуется от MediaLibraryService (media3) - раньше MediaBrowserServiceCompat.
 * Медиасессия (MediaLibrarySession) соединяет плеер с экраном приложения (MusicServiceConnection), уведомлением,
 * наушниками и т.п. Уведомление и foreground-режим сервиса media3 ведёт сама (раньше - PlayerNotificationManager
 * и MusicPlayerNotificationListener).
 * Создаём CoroutineScope для задач, решаемых в сервисе, чтобы не перегружать наш main thread (don't forget serviceScope.cancel() in onDestroy!)
 * Fetching our metadata from our class, created earlier (firebaseMusicSource)
 */
@UnstableApi // setForegroundServiceTimeoutMs, setShowNotificationForIdlePlayer, DefaultMediaNotificationProvider.setSmallIcon
@AndroidEntryPoint
class MusicService : MediaLibraryService() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

    @Inject
    lateinit var firebaseMusicSource: FirebaseMusicSource

    @Inject
    lateinit var preference: IAppSharedPreference

    // Create a coroutine scope to avoid using main thread for our tasks
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaLibrarySession: MediaLibrarySession

    private lateinit var musicPlayerEventListener: MusicPlayerEventListener

    private lateinit var notifyChildrenChangedLiveDataObserver: Observer<Boolean>

    private val intent =
        Intent(Constants.FILTER_FOR_BROADCAST_MA) // FILTER is a string to identify this intent
    private lateinit var listSizeLiveDataObserver: Observer<Int>
    private lateinit var radioStationsCountLiveDataObserver: Observer<Int>
    private val intentServerIsDown =
        Intent(Constants.FILTER_FOR_BROADCAST_MA_SERVER) // FILTER is a string to identify this intent
    private lateinit var serverIsDownLiveDataObserver: Observer<Boolean>

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

        serviceScope.launch {
            try {
                // Узнать, какой country code был у последней радиостанции при последней запуске, если это не первый запуск. Если первый запуск - запустить по умолчанию
                val lastPlayedCountryCode = preference.getLastUsedRadioStationCountryCode()
                Log.d(TAG, "Узнаём последний используемый код страны - $lastPlayedCountryCode")

                // Загружаем метаданные всех радиостанций с определенным country code ПРИ ЗАПУСКЕ СЕРВИСА
                if (lastPlayedCountryCode.endsWith("_FAV", true)) {
                    firebaseMusicSource.fetchFavouriteMediaData()
                    Log.d(TAG, "Загружаем метаданные fetchMediaData - FAV")
                } else {
                    firebaseMusicSource.fetchMediaData(
                        if (lastPlayedCountryCode.isNotBlank() && lastPlayedCountryCode != "null") {
                            lastPlayedCountryCode
                        } else {
                            DEFAULT_COUNTRY_CODE
                        }
                    )
                    Log.d(
                        TAG,
                        "Загружаем метаданные, fetchMediaData - $lastPlayedCountryCode. Если lastPlayedCountryCode пуст, то загружается $DEFAULT_COUNTRY_CODE"
                    )
                }

            } catch (e: SocketTimeoutException) {
                Log.d(
                    TAG,
                    "! Caught SocketTimeoutException in method firebaseMusicSource.fetchMediaData()"
                )
                e.printStackTrace()
            } catch (e: IOException) {
                // Когда сохранён не верный CountryCode, по запросу такого не найдёт и выдаст ошибку retrofit2.HttpException: HTTP 404
                Log.d(TAG, "! Caught IOException in method firebaseMusicSource.fetchMediaData()")
                e.printStackTrace()
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
            firebaseMusicSource = firebaseMusicSource,
            serviceScope = serviceScope,
            player = exoPlayer,
            getLastUsedRadioStationUrl = { preference.getLastUsedRadioStationUrl() },
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
        // (FirebaseMusicSource.toRadioStationMediaItem)
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

        // Observing notifyChildrenChangedLiveData from firebase in service
        notifyChildrenChangedLiveDataObserver = Observer<Boolean> {
            // Скачан новый плейлист - экран (MusicServiceConnection) запросит список станций заново
            mediaLibrarySession.notifyChildrenChanged(
                MEDIA_ROOT_ID,
                firebaseMusicSource.radioStations.size,
                null
            )
        }
        firebaseMusicSource.notifyChildrenChangedLiveData.observeForever(
            notifyChildrenChangedLiveDataObserver
        )

        // List size for broadcast
        listSizeLiveDataObserver = Observer {
            //Live data value has changed
            intent.putExtra(KEY_BROADCAST_LIST_SIZE_MA, it)
            Log.d(
                TAG,
                "BROADCAST: Отправляем в MainActivity данные из listSizeLiveDataObserver. it = $it"
            )
            sendBroadcast(intent)
        }
        firebaseMusicSource.listSizeLiveData.observeForever(listSizeLiveDataObserver)

        // Counting for broadcast
        radioStationsCountLiveDataObserver = Observer {
            intent.putExtra(KEY_BROADCAST_COUNT_MA, it)
            Log.d(
                TAG,
                "BROADCAST: Отправляем в MainActivity данные из radioStationsCountLiveDataObserver"
            )
            sendBroadcast(intent)
        }
        firebaseMusicSource.radioStationsCountLiveData.observeForever(
            radioStationsCountLiveDataObserver
        )

        // Вызывается в случае ошибки HttpException при обращении к серверу
        serverIsDownLiveDataObserver = Observer {
            intentServerIsDown.apply {
                putExtra(KEY_BROADCAST_SERVER_IS_DOWN, it)
            }
            Log.d(TAG, "LocalBroadcastManager.BROADCAST: Отправляем в MainActivity данные из serverIsDownLiveDataObserver, it = $it")
            LocalBroadcastManager.getInstance(this).sendBroadcast(intentServerIsDown)
        }
        firebaseMusicSource.serverIsDownLiveData.observeForever(
            serverIsDownLiveDataObserver
        )

        musicPlayerEventListener = MusicPlayerEventListener(this)
        exoPlayer.addListener(musicPlayerEventListener)
        exoPlayer.addListener(pauseTimeoutListener)

//        // 3.Broadcast для завершения сервиса (1 - в MainActivity)
//        registerReceiver(receiver, IntentFilter(FILTER_FOR_BROADCAST_MS))
    }

    // Запущенный сервис будет работать пока у него не вызван stopSelf().
    // Передавать данные в сервис можно так же с помощью startService(intent),
    // новый сервис запускаться при этом не будет, а у запущенного сервиса будет вызван onStartCommand.

    fun testMethodForError(errorCause: String) {
        when (errorCause) {
            "UnrecognizedInputFormatException" -> {
                Log.d(TAG, "")
            }

            "HttpDataSourceException" -> {
                Log.d(TAG, "")
            }

            else -> {
                Log.d(TAG, "")
            }
        }
    }

    private fun isPlayingOrStarting(): Boolean =
        exoPlayer.playWhenReady &&
                (exoPlayer.playbackState == Player.STATE_BUFFERING || exoPlayer.playbackState == Player.STATE_READY)

    // Сессия для подключающихся контроллеров (экран приложения, уведомление, наушники)
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession = mediaLibrarySession

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

        firebaseMusicSource.notifyChildrenChangedLiveData.removeObserver(notifyChildrenChangedLiveDataObserver)
        firebaseMusicSource.listSizeLiveData.removeObserver(listSizeLiveDataObserver)
        firebaseMusicSource.radioStationsCountLiveData.removeObserver(radioStationsCountLiveDataObserver)
        firebaseMusicSource.serverIsDownLiveData.removeObserver(serverIsDownLiveDataObserver)

//        // 3.Broadcast - регистрируем в onCreate и отписываемся в onDestroy
//        unregisterReceiver(receiver)

        super.onDestroy()
    }

//    // 2.Broadcast для управления уведомлением из Activity (1 - в MainActivity)
//    // Создадим анонимный класс => не нужно регистрировать в манифесте
//    private var receiver: BroadcastReceiver? = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent) {
//            Log.d(TAG, "Получен ключ из Activity в BroadcastReceiver")
//
////            if (numberFromActivity == 100) {
////                Log.d(TAG, "Получен ключ KEY_BROADCAST_ACTIVITY_DESTROYED, число 100 - убираем уведомление")
//////                onDestroy()
////                cancelNotifications()
////            }
//
//            when (intent.getIntExtra(Constants.KEY_BROADCAST_ACTIVITY, 1)) {
//                50 -> {
//                    Log.d(TAG, "Получен ключ KEY_BROADCAST_ACTIVITY, число 50 - показываем уведомление")
//                    musicNotificationManager.showNotification(exoPlayer)
//                }
//                100 -> {
//                    Log.d(TAG, "Получен ключ KEY_BROADCAST_ACTIVITY, число 100 - убираем уведомление")
//                    musicNotificationManager.cancelNotifications()
//                }
//            }
//        }
//    }
}