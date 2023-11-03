package com.myproject.radiojourney.utils.exoplayer

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.lifecycle.Observer
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.myproject.radiojourney.data.sharedPreference.IAppSharedPreference
import com.myproject.radiojourney.other.Constants
import com.myproject.radiojourney.other.Constants.KEY_BROADCAST_COUNT_MA
import com.myproject.radiojourney.other.Constants.KEY_BROADCAST_LIST_SIZE_MA
import com.myproject.radiojourney.other.Constants.MEDIA_ROOT_ID
import com.myproject.radiojourney.other.Constants.NETWORK_ERROR
import com.myproject.radiojourney.utils.exoplayer.callback.MusicPlaybackPreparer
import com.myproject.radiojourney.utils.exoplayer.callback.MusicPlayerEventListener
import com.myproject.radiojourney.utils.exoplayer.callback.MusicPlayerNotificationListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject


/**
 * Создадим наш Exoplayer и сервис для него.
 * Создаём MusicService и наследуемся от MediaBrowserServiceCompat - it's like a Service for Media
 * Создаём CoroutineScope для задач, решаемых в сервисе, чтобы не перегружать наш main thread (don't forget serviceScope.cancel() in onDestroy!)
 * Инициализируем наши переменные, а так же необходимые intent-ы в onCreate.
 * Fetching our metadata from our class, created earlier (firebaseMusicSource)
 * Create an inner class MusicQueueNavigator
 * Describe functions onGetRoot and onLoadChildren
 */
private const val SERVICE_TAG = "MusicService"

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    // Inject our data source factory
    @Inject
    lateinit var dataSourceFactory: DefaultDataSource.Factory

    @Inject
    lateinit var httpDataSourceFactory: DefaultHttpDataSource.Factory

    @Inject
    lateinit var exoPlayer: ExoPlayer // SimpleExoPlayer is deprecated

    @Inject
    lateinit var firebaseMusicSource: FirebaseMusicSource

    @Inject
    lateinit var preference: IAppSharedPreference

    // Create a coroutine scope to avoid using main thread for our tasks
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector // a class for connecting to media session

    private lateinit var musicNotificationManager: MusicNotificationManager

    var isForegroundService = false // it will be needed for our exoplayer notificationListener

    private var curPlayingSong: MediaMetadataCompat? = null
    private var isPlayerInitialized = false

    private lateinit var musicPlayerEventListener: MusicPlayerEventListener

    private lateinit var notifyChildrenChangedLiveDataObserver: Observer<Boolean>

    val intent =
        Intent(Constants.FILTER_FOR_BROADCAST_MA) // FILTER is a string to identify this intent
    private lateinit var listSizeLiveDataObserver: Observer<Int>
    private lateinit var radioStationsCountLiveDataObserver: Observer<Int>

    companion object {
        private const val TAG = "MusicService"

        var curSongDuration = 0L
            private set // <- !!! means that we can set it only here, but we can read it elsewhere
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
                        if (lastPlayedCountryCode != "null" && lastPlayedCountryCode.isNotBlank()) lastPlayedCountryCode
                        else "AD"
                    )
                    Log.d(TAG, "Загружаем метаданные fetchMediaData - $lastPlayedCountryCode")
                }

            } catch (e: SocketTimeoutException) {
                // Когда сохранён не верный CountryCode, по запросу такого не найдёт и выдаст ошибку retrofit2.HttpException: HTTP 404
                Log.d(
                    TAG,
                    "! Caught SocketTimeoutException in method firebaseMusicSource.fetchMediaData()"
                )
                e.printStackTrace()
//                firebaseMusicSource.fetchMediaData("AD")
                // TODO в этом случае лучше ничего не скачивать и выдать диалоговое окно об ошибке
            } catch (e: IOException) {
                // Когда сохранён не верный CountryCode, по запросу такого не найдёт и выдаст ошибку retrofit2.HttpException: HTTP 404
                Log.d(
                    TAG,
                    "! Caught IOException in method firebaseMusicSource.fetchMediaData()"
                )
                e.printStackTrace()
//                firebaseMusicSource.fetchMediaData("AD")
                // TODO в этом случае лучше ничего не скачивать и выдать диалоговое окно об ошибке
            }
        }

        // Pending intent for opening our activity when we click on notification
        val openActivityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_MUTABLE)
            } else {
                PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_UPDATE_CURRENT)
            }
        }

        // Media session comes with token. We can use this token to get some information about this media session
        mediaSession = MediaSessionCompat(this, SERVICE_TAG).apply {
            setSessionActivity(openActivityIntent)
            isActive = true
        }

        // Now we need to sing our media token to our service
        sessionToken = mediaSession.sessionToken

        // lambda in this {} will be switched every time, when a new song begins
        musicNotificationManager = MusicNotificationManager(
            this,
            mediaSession.sessionToken,
            MusicPlayerNotificationListener(this)
        ) {
            // here we can update the current duration of the song that is playing
//            curSongDuration = exoPlayer.duration
            if (exoPlayer.duration != C.TIME_UNSET) {
                curSongDuration = exoPlayer.duration
            }
        }

        // lambda in this {} will be switched every time, when user chooses a new song
        val musicPlaybackPreparer = MusicPlaybackPreparer(firebaseMusicSource, serviceScope) {

            if (isPlayerInitialized && it == null) {
                Log.d(
                    TAG,
                    "PLAYLIST_UPDATE: 5.$TAG, MediaMetadataCompat == null, выходим из лямбды musicPlaybackPreparer."
                )
                return@MusicPlaybackPreparer // Если isPlayerInitialized == true, значит это точно не первый запуск. Если isPlayerInitialized && it == null - значит сюда передан результат раньше, чем скачался плейлист (Было curPlayingSong != null && it == null, работает с нюансами)
            }
            curPlayingSong = it
            preparePlayer(
                firebaseMusicSource.radioStations,
                it,
                true
            )
            Log.d(
                TAG,
                "PLAYLIST_UPDATE: 5.$TAG, MediaMetadataCompat передана, вызываем preparePlayer()"
            )
        }

        // Observing notifyChildrenChangedLiveData from firebase in service
        notifyChildrenChangedLiveDataObserver = Observer<Boolean> {
            //Live data value has changed
            notifyChildrenChanged(MEDIA_ROOT_ID)
        }
        firebaseMusicSource.notifyChildrenChangedLiveData.observeForever(
            notifyChildrenChangedLiveDataObserver
        )

        // List size for broadcast
        listSizeLiveDataObserver = Observer {
            //Live data value has changed
            intent.putExtra(KEY_BROADCAST_LIST_SIZE_MA, it)
            Log.d(TAG, "BROADCAST: Отправляем в MainActivity данные из listSizeLiveDataObserver")
            sendBroadcast(intent)
        }
        firebaseMusicSource.listSizeLiveData.observeForever(listSizeLiveDataObserver)

        // Counting for broadcast
        radioStationsCountLiveDataObserver = Observer {
            //Live data value has changed
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

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(musicPlaybackPreparer) // 11.
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator()) // 14.2
        mediaSessionConnector.setPlayer(exoPlayer)


        musicPlayerEventListener = MusicPlayerEventListener(this)
        exoPlayer.addListener(musicPlayerEventListener)
        musicNotificationManager.showNotification(exoPlayer)

//        // 3.Broadcast для завершения сервиса (1 - в MainActivity)
//        registerReceiver(receiver, IntentFilter(FILTER_FOR_BROADCAST_MS))
    }

    // Запущенный сервис будет работать пока у него не вызван stopSelf().
    // Передавать данные в сервис можно так же с помощью startService(intent),
    // новый сервис запускаться при этом не будет, а у запущенного сервиса будет вызван onStartCommand.

    fun testMethodForError() {

    }

    // Let's prepare our exoplayer
    private fun preparePlayer(
        radioStations: List<MediaMetadataCompat>,
        itemToPlay: MediaMetadataCompat?,
        playNow: Boolean
    ) {
        var lastItemIndex = 0

//        val test = curPlayingSong?.description?.subtitle
//        val testRadioStations = radioStations

        // Если мы только что запустили программу, то песня ещё не будет выбрана. Стоит отобразить в плейере ту, что была выбрана последней в предыдущем запуске
        // Если будет глючить, возможно стоит попробовать ориентироваться на isPlayerInitialized, а не curPlayingSong (Проверила, у меня работало нормально)
        if (curPlayingSong == null) {
            // Находим её mediaId (after updating database, we're looking for url, not mediaId)
            val lastUsedRadioStationUrl = preference.getLastUsedRadioStationUrl()
            var radioStationNeedToFind: MediaMetadataCompat? = null

            // Находим станцию по mediaId (after updating database, we're looking for url, not mediaId)
            if (lastUsedRadioStationUrl != "") {
                radioStations.forEach {
                    if (it.description.mediaUri.toString() == lastUsedRadioStationUrl) {
                        radioStationNeedToFind = it
                    }
                }
            }

            // Если станция нашлась, находим её индекс для плейера. Если не нашлась - оставляем значение 0 (просто первая в списке)
            radioStationNeedToFind?.let {
                // looking for the index of last listened song
                lastItemIndex = radioStations.indexOf(radioStationNeedToFind)
                // That function will return -1 if the song doesn't exist, so we must check:
                if (lastItemIndex == -1) lastItemIndex = 0
            }
        }

        val curSongIndex =
            if (curPlayingSong == null) lastItemIndex else radioStations.indexOf(itemToPlay) // если песня не выбрана - просто играем первую. Либо ищем конкретную по индексу


        serviceScope.launch {

            if (radioStations.isNotEmpty() && curSongIndex < firebaseMusicSource.radioStations.size) {
                // Проверить, заканчивается ли ссылка на .m3u8
                // Если да, нам нужно использовать HlsMediaSource
                val mediaUri =
                    firebaseMusicSource.radioStations[curSongIndex].description.mediaUri.toString()
                if (mediaUri.endsWith(".m3u8")
                ) {
                    // TODO Player is accessed on the wrong thread.
                    exoPlayer.setMediaSource(
                        firebaseMusicSource.asHlsMediaSource(
                            httpDataSourceFactory
                        )
                    ) // Вызываем метод из firebaseMusicSource, чтобы сформировать данные для плейлист
                    // TODO Так мы исправили ошибку UnrecognizedInputFormatException, но только если станция запущена из viewpager. Если до такой станции дошли через кнопки в уведомлении, станция играть не будет.
                } else {
                    // TODO Player is accessed on the wrong thread.
                    // ExoPlayer.prepare(MediaSource mediaSource) is deprecated. Use setMediaSource(MediaSource) and ExoPlayer.prepare() instead
                    exoPlayer.setMediaSource(firebaseMusicSource.asMediaSource(dataSourceFactory)) // Вызываем метод из firebaseMusicSource, чтобы сформировать данные для плейлист
                }
                Log.d(
                    TAG,
                    "PLAYLIST_UPDATE: 5.$TAG, preparePlayer(). Проверяем, заканчивается ли ссылка на .m3u8. Вызываем метод мз firebaseMusicSource, чтобы сформировать данные для плейлист"
                )
            }

            exoPlayer.seekTo(
                curSongIndex,
                0L
            ) // start curSongIndex song, that we choose. 0L = from the beginning
            exoPlayer.playWhenReady =
                playNow // play song, when it will be ready (it will be false, and after - true, when ready)

            exoPlayer.prepare()
            Log.d(
                TAG,
                "PLAYLIST_UPDATE: 5.$TAG, preparePlayer(). Находим нужную станцию и включаем плейер"
            )

        }
    }

    // media root id - is the id to the very first media item (what should be shown first)
// here we also can deny clients connect to a specific id
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
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
            // first subscription in our app. ПЕРВАЯ ЗАГРУЗКА, media ID по умолчанию - MEDIA_ROOT_ID
            MEDIA_ROOT_ID -> {
                val resultsSent = firebaseMusicSource.whenReady { isInitialized ->
                    if (isInitialized) {

                        try {
                            result.sendResult(firebaseMusicSource.asMediaItems())
                            // we also must check, if our player is initialized
                            if (!isPlayerInitialized && firebaseMusicSource.radioStations.isNotEmpty()) {
                                preparePlayer(
                                    firebaseMusicSource.radioStations,
                                    firebaseMusicSource.radioStations[0],
                                    false
                                )
                                isPlayerInitialized = true
                                Log.d(
                                    TAG,
                                    "PLAYLIST_UPDATE: 5.$TAG, вызываем preparePlayer() из onLoadChildren()"
                                )
                            }
                        } catch (exception: Exception) {
                            // not recommend to notify here , instead notify when you
                            // change existing list in MusicPlaybackPreparer onCommand()
                            notifyChildrenChanged(MEDIA_ROOT_ID)

                            Log.d(
                                TAG,
                                "PLAYLIST_UPDATE: Exception in fun onLoadChildren(), MEDIA_ROOT_ID"
                            )
                            exception.printStackTrace()
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

    // when the task of the service has been removed (when the intent has been removed)
    override fun onTaskRemoved(rootIntent: Intent?) {
        exoPlayer.stop()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Log.d(TAG, "MUSIC SERVICE IS DESTROYED -> вызван метод onDestroy()")

        serviceScope.cancel()

        exoPlayer.removeListener(musicPlayerEventListener)
        exoPlayer.release()
        firebaseMusicSource.notifyChildrenChangedLiveData.removeObserver(
            notifyChildrenChangedLiveDataObserver
        )

//        // 3.Broadcast - регистрируем в onCreate и отписываемся в onDestroy
//        unregisterReceiver(receiver)

        super.onDestroy()
    }

    // It will be called once our service needs new description from media item
    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(
            player: Player,
            windowIndex: Int
        ): MediaDescriptionCompat {
//            return firebaseMusicSource.radioStations[windowIndex].description // windowIndex - index of the song that is now playing

            // TODO Иногда выдаёт ошибку, например java.lang.IndexOutOfBoundsException: Index: 14, Size: 4. Пока сделаю так, не знаю как исправить, null передать нельзя
            return if (windowIndex < firebaseMusicSource.radioStations.size) {
                firebaseMusicSource.radioStations[windowIndex].description // windowIndex - index of the song that is now playing
            } else {
                firebaseMusicSource.radioStations[0].description
            }
        }
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