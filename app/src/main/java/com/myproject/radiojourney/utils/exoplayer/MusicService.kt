package com.myproject.radiojourney.utils.exoplayer

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.myproject.radiojourney.other.Constants.MEDIA_ROOT_ID
import com.myproject.radiojourney.other.Constants.NETWORK_ERROR
import com.myproject.radiojourney.utils.exoplayer.callback.MusicPlaybackPreparer
import com.myproject.radiojourney.utils.exoplayer.callback.MusicPlayerEventListener
import com.myproject.radiojourney.utils.exoplayer.callback.MusicPlayerNotificationListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Создадим наш Exoplayer и сервис для него.
 * 1. Создаём MusicService и наследуемся от MediaBrowserServiceCompat - it's like a Service for Media
 * 2. Имплементируем методы. Опишем их тело позже
 * 3. Внедряем необходимые зависимости
 * 4. Создаём CoroutineScope для задач, решаемых в сервисе, чтобы не перегружать наш main thread (don't forget serviceScope.cancel() in onDestroy!)
 * 5. Инициализируем наши переменные, а так же необходимые intent-ы в onCreate.
 * ...
 * 6. Добавляем переменные isForegroundService и musicNotificationManager
 * 7. Инициализируем musicNotificationManager. Lambda in this {} will be switched every time, when a new song begins;
 * musicNotificationManager.showNotification(exoPlayer)
 * 8. MusicPlaybackPreparer, MusicPlayerEventListener
 * 9. A variable of currentPlayingSong
 * 10. Initialize musicPlaybackPreparer in onCreate
 * 11. mediaSessionConnector.setPlaybackPreparer(musicPlaybackPreparer)
 * 12. exoPlayer.addListener(MusicPlayerEventListener(this))
 * 13. fetching our metadata from our class, created earlier (firebaseMusicSource)
 * 14. Create an inner class MusicQueueNavigator
 * 15. Describe functions onGetRoot and onLoadChildren
 */
private const val SERVICE_TAG = "MusicService"

// 1.
@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    // 3.
    // Inject our data source factory
    @Inject
    lateinit var dataSourceFactory: DefaultDataSourceFactory

    @Inject
    lateinit var exoPlayer: SimpleExoPlayer

    @Inject
    lateinit var firebaseMusicSource: FirebaseMusicSource

    // 4.
    // Create a coroutine scope to avoid using main thread for our tasks
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector // a class for connecting to media session

    // 6.
    private lateinit var musicNotificationManager: MusicNotificationManager

    var isForegroundService = false // it will be needed for our exoplayer notificationListener

    // 9.
    private var curPlayingSong: MediaMetadataCompat? = null

    // 15.2
    private var isPlayerInitialized = false

    private lateinit var musicPlayerEventListener: MusicPlayerEventListener

    companion object {
        var curSongDuration = 0L
            private set // <- !!! means that we can set it only here, but we can read it elsewhere
    }

    // 2.
    override fun onCreate() {
        super.onCreate()
        // 13.
        serviceScope.launch {
            firebaseMusicSource.fetchMediaData() // Загрузаем метаданные всех радиостанций с сервера
        }

        // 5.
        // Pending intent for opening our activity when we click on notification
        val openActivityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, 0)
        }

        // Media session comes with token. We can use this token to get some information about this media session
        mediaSession = MediaSessionCompat(this, SERVICE_TAG).apply {
            setSessionActivity(openActivityIntent)
            isActive = true
        }

        // Now we need to sing our media token to our service
        sessionToken = mediaSession.sessionToken

        // 7.
        // lambda in this {} will be switched every time, when a new song begins
        musicNotificationManager = MusicNotificationManager(
            this,
            mediaSession.sessionToken,
            MusicPlayerNotificationListener(this)
        ) {
            // here we can update the current duration of the song that is playing
            curSongDuration = exoPlayer.duration
        }

        // 10.
        // lambda in this {} will be switched every time, when user chooses a new song
        val musicPlaybackPreparer = MusicPlaybackPreparer(firebaseMusicSource) {
            curPlayingSong = it
            preparePlayer(
                firebaseMusicSource.radioStations,
                it,
                true
            )
        }

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(musicPlaybackPreparer) // 11.
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator()) // 14.2
        mediaSessionConnector.setPlayer(exoPlayer)

        // 12.
        musicPlayerEventListener = MusicPlayerEventListener(this)
        exoPlayer.addListener(musicPlayerEventListener)
        musicNotificationManager.showNotification(exoPlayer)
    }

    // 14.1
    // It will be called once our service needs new description from media item
    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return firebaseMusicSource.radioStations[windowIndex].description // windowIndex - index of the song that is now playing
        }
    }

    // Let's prepare our exoplayer
    private fun preparePlayer(
        radioStations: List<MediaMetadataCompat>,
        itemToPlay: MediaMetadataCompat?,
        playNow: Boolean
    ) {
        val curSongIndex =
            if (curPlayingSong == null) 0 else radioStations.indexOf(itemToPlay) // если песня не выбрана - просто играем первую. Либо ищем конкретную по индексу
        exoPlayer.prepare(firebaseMusicSource.asMediaSource(dataSourceFactory)) // Вызываем метод из firebaseMusicSource, чтобы сформировать плейлист
        exoPlayer.seekTo(
            curSongIndex,
            0L
        ) // start curSongIndex song, that we choose. 0L = from the beginning
        exoPlayer.playWhenReady =
            playNow // play song, when it will be ready (it will be false, and after - true, when ready)
    }

    // when the task of the service has been removed (when the intent has been removed)
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()

        exoPlayer.removeListener(musicPlayerEventListener)
        exoPlayer.release()
    }

    // 15.1
    // media root id - is the id to the very first media item (what should be shown first)
    // here we also can deny clients connect to a specific id
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(MEDIA_ROOT_ID, null)
    }

    // You can think of this music app as about some file manager - you can navigate through folders and you also have some files there.
    // In our example files - are songs. Folders are albums, playlists and so on. (MediaBrowserCompat.MediaItem can be as playable song, so a browsable album)
    // Open playlists on click, open related albums on album click and so on. This must be described here
    // P.S. Each of our playlists has it's own id. Client's can subscribe on those ids (play the items on just specific playlists). In this method we can check this
    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        // When we subscribe on parentId we must send a corresponding result with the songs of the MediaItems in that parentId
        // So, we subscribe to a specific playlist, than we pass the playlists parentId here. And in this function we use this result to give the result of MutableList<MediaBrowserCompat.MediaItem> inside of that playlist (be basically send that back)

        // in our example we only have that root ID. If there will be more - add them in WHEN expression
        when (parentId) {
            // first subscription in our app
            MEDIA_ROOT_ID -> {
                val resultsSent = firebaseMusicSource.whenReady { isInitialized ->
                    if (isInitialized) {
                        result.sendResult(firebaseMusicSource.asMediaItems())
                        // we also must check, if our player is initialized
                        if (!isPlayerInitialized && firebaseMusicSource.radioStations.isNotEmpty()) {
                            preparePlayer(
                                firebaseMusicSource.radioStations,
                                firebaseMusicSource.radioStations[0],
                                false
                            )
                            isPlayerInitialized = true
                        }
                        // if it is ready, but not initialized:
                    } else {
                        // If the result is null - we caught a network error
                        // Where we will set the NETWORK_ERROR here, so that we can catch it in MusicServiceConnection
                        mediaSession.sendSessionEvent(NETWORK_ERROR, null)
                        result.sendResult(null)
                    }
                }
                // if result have not been sent here - result.detach() to check later part if this is actually sent here
                if (!resultsSent) {
                    result.detach()
                }
            }
        }
    }
}