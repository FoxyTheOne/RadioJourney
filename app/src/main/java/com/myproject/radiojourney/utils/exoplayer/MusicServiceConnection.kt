package com.myproject.radiojourney.utils.exoplayer

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.myproject.radiojourney.other.Constants.NETWORK_ERROR
import com.myproject.radiojourney.other.Event
import com.myproject.radiojourney.other.Resource

/**
 * A class for connection between activity or fragment with MusicService.
 * С media3 подключение к сервису - через MediaBrowser (раньше MediaBrowserCompat + MediaControllerCompat).
 * MediaBrowser - это одновременно и "пульт" плеера (play, pause, выбор станции), и доступ к списку станций.
 *
 * MediaBrowser работает только в главном потоке, поэтому все команды отправляются через mainHandler:
 * их можно вызывать из любого потока (например, из viewModelScope.launch(Dispatchers.IO) в MainViewModel).
 */
class MusicServiceConnection(private val context: Context) {
    companion object {
        private const val TAG = "MusicServiceConnection"
    }

    // LiveData for our Service, where we will keep data (data for our fragments to update if server changes)
    private val _isConnectedLiveData =
        MutableLiveData<Event<Resource<Boolean>>>() // For current state. Event and Resource - are our classes
    val isConnectedLiveData: LiveData<Event<Resource<Boolean>>> = _isConnectedLiveData

    private val _networkErrorLiveData =
        MutableLiveData<Event<Resource<Boolean>>>() // It must be private, so that other classes can't change it
    val networkErrorLiveData: LiveData<Event<Resource<Boolean>>> =
        _networkErrorLiveData // And another LiveData, that equals to previous, so that classes can't change it

    // Is player playing or not
    private val _playbackStateLiveData = MutableLiveData<PlaybackStateInfo?>()
    val playbackStateLiveData: LiveData<PlaybackStateInfo?> = _playbackStateLiveData

    // Станция, которая сейчас в плеере: mediaId (stationuuid), mediaMetadata.title (название),
    // mediaMetadata.subtitle (код страны, "PL" или "PL_FAV")
    private val _curPlayingSongLiveData = MutableLiveData<MediaItem?>()
    val curPlayingSongLiveData: LiveData<MediaItem?> = _curPlayingSongLiveData

    private val mainHandler = Handler(Looper.getMainLooper())

    private var mediaBrowser: MediaBrowser? = null
    private var isConnecting = false

    // Команды, отправленные до подключения к сервису - выполняются сразу после подключения
    private val pendingActions = mutableListOf<(MediaBrowser) -> Unit>()

    // Подписчики на списки станций: parentId -> получатели списка
    private val childrenSubscribers = mutableMapOf<String, MutableList<(List<MediaItem>) -> Unit>>()

    // Чтобы не отправлять на экран одну и ту же станцию повторно (при каждом обновлении Timeline):
    // экран реагирует на смену станции (прячет полосу загрузки, обновляет звезду и т.п.)
    private var lastPostedSongKey: String? = null

    private val browserListener = object : MediaBrowser.Listener {
        // Скачан новый плейлист (MusicService -> notifyChildrenChanged) - запрашиваем список станций заново
        override fun onChildrenChanged(
            browser: MediaBrowser,
            parentId: String,
            itemCount: Int,
            params: androidx.media3.session.MediaLibraryService.LibraryParams?
        ) {
            loadChildren(browser, parentId)
        }

        // Send custom events from our service to this connection. We use it to notify when there is a network error
        override fun onCustomCommand(
            controller: MediaController,
            command: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (command.customAction == NETWORK_ERROR) postNetworkError()
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        // Invoked when the client is disconnected from the media session
        // (например, сервис завершился). При следующей команде подключимся заново
        override fun onDisconnected(controller: MediaController) {
            mediaBrowser = null
            _isConnectedLiveData.postValue(Event(Resource.error("The connection was suspended", false)))
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            postPlaybackState(player)
            if (events.containsAny(
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_MEDIA_METADATA_CHANGED
                )
            ) {
                postCurrentSong(player)
            }
        }
    }

    init {
        mainHandler.post { connect() }
    }

    // Подключаемся к MusicService (в главном потоке)
    private fun connect() {
        if (isConnecting || mediaBrowser != null) return
        isConnecting = true
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        val browserFuture = MediaBrowser.Builder(context, sessionToken)
            .setListener(browserListener)
            .buildAsync()
        browserFuture.addListener({
            isConnecting = false
            try {
                val browser = browserFuture.get()
                mediaBrowser = browser
                browser.addListener(playerListener)
                postPlaybackState(browser)
                postCurrentSong(browser)
                _isConnectedLiveData.postValue(Event(Resource.success(true))) // post connection data to LiveData

                // После переподключения подписываемся на списки станций заново
                childrenSubscribers.keys.forEach { browser.subscribe(it, null) }

                val actions = pendingActions.toList()
                pendingActions.clear()
                actions.forEach { it(browser) }
            } catch (e: Exception) {
                Log.d(TAG, "Couldn't connect to MusicService: ${e.message}")
                _isConnectedLiveData.postValue(Event(Resource.error("Couldn't connect to media browser", false)))
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Выполнить действие с MediaBrowser в главном потоке; если ещё не подключились - после подключения
    private fun withBrowser(action: (MediaBrowser) -> Unit) {
        mainHandler.post {
            val browser = mediaBrowser
            if (browser != null && browser.isConnected) {
                action(browser)
            } else {
                pendingActions += action
                connect()
            }
        }
    }

    // Also let's create functions for subscribing and unsubscribing (for calling from view model):
    fun subscribe(parentId: String, onChildrenLoaded: (List<MediaItem>) -> Unit) {
        withBrowser { browser ->
            childrenSubscribers.getOrPut(parentId) { mutableListOf() } += onChildrenLoaded
            // После подписки сессия сама сообщит onChildrenChanged, и список будет загружен (loadChildren)
            browser.subscribe(parentId, null)
        }
    }

    fun unsubscribe(parentId: String, onChildrenLoaded: (List<MediaItem>) -> Unit) {
        withBrowser { browser ->
            val subscribers = childrenSubscribers[parentId] ?: return@withBrowser
            subscribers.remove(onChildrenLoaded)
            if (subscribers.isEmpty()) {
                childrenSubscribers.remove(parentId)
                browser.unsubscribe(parentId)
            }
        }
    }

    private fun loadChildren(browser: MediaBrowser, parentId: String) {
        val childrenFuture = browser.getChildren(parentId, /* page= */ 0, /* pageSize= */ Int.MAX_VALUE, null)
        childrenFuture.addListener({
            val result = try {
                childrenFuture.get()
            } catch (e: Exception) {
                Log.d(TAG, "getChildren($parentId) failed: ${e.message}")
                null
            }
            val items = result?.value
            if (result?.resultCode == LibraryResult.RESULT_SUCCESS && items != null) {
                childrenSubscribers[parentId]?.toList()?.forEach { it(items) }
            }
        }, mainHandler::post)
    }

    // Controls (раньше transportControls)
    fun play() = withBrowser { it.play() } // если плеер остановлен (STATE_IDLE), сессия сама подготовит его

    fun pause() = withBrowser { it.pause() }

    // Включить станцию по stationuuid (раньше transportControls.playFromMediaId). Сессия (MusicLibrarySessionCallback.onSetMediaItems)
    // заменит этот MediaItem на весь плейлист и начнёт с нужной станции
    fun playFromMediaId(mediaId: String) = withBrowser { browser ->
        browser.setMediaItem(MediaItem.Builder().setMediaId(mediaId).build())
        browser.prepare()
        browser.play()
    }

    // Команды сервису: загрузить плейлист, отменить загрузку
    fun sendCommand(command: String, parameters: Bundle?) = withBrowser { browser ->
        browser.sendCustomCommand(SessionCommand(command, Bundle.EMPTY), parameters ?: Bundle.EMPTY)
    }

    private fun postPlaybackState(player: Player) {
        _playbackStateLiveData.postValue(
            PlaybackStateInfo(
                playbackState = player.playbackState,
                playWhenReady = player.playWhenReady,
                isActuallyPlaying = player.isPlaying,
                hasError = player.playerError != null,
                updateTime = SystemClock.elapsedRealtime()
            )
        )
    }

    private fun postCurrentSong(player: Player) {
        val currentItem = player.currentMediaItem
        // Та же станция из другого плейлиста (например, из избранного: subtitle "PL_FAV") - это другая запись
        val key = currentItem?.let { "${it.mediaId}|${it.mediaMetadata.subtitle}" }
        if (key == lastPostedSongKey) return
        lastPostedSongKey = key
        _curPlayingSongLiveData.postValue(currentItem) // Getting new meta data (put it into LiveData)
    }

    private fun postNetworkError() {
        _networkErrorLiveData.postValue(
            Event(Resource.error("Couldn't connect to the server. Please check your internet connection.", null))
        )
    }
}