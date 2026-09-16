package com.myproject.radiojourney.utils.exoplayer

import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.*
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.ShuffleOrder
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.myproject.radiojourney.data.dataSource.network.INetworkRadioDataSource
import com.myproject.radiojourney.data.localDatabaseRoom.IRadioStationDAO
import com.myproject.radiojourney.other.Status
import com.myproject.radiojourney.utils.exoplayer.State.*
import com.myproject.radiojourney.utils.extension.call
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

// We need time to upload music from firebase or other data
class FirebaseMusicSource @Inject constructor(
    private val networkRadioDataSource: INetworkRadioDataSource,
    private val radioStationDAO: IRadioStationDAO
) {
    companion object {
        private const val TAG = "FirebaseMusicSource"
    }

    private val _notifyChildrenChangedLiveData = MutableLiveData<Boolean>()
    val notifyChildrenChangedLiveData: LiveData<Boolean> =
        _notifyChildrenChangedLiveData

    private val _listSizeLiveData = MutableLiveData<Int>()
    val listSizeLiveData: LiveData<Int> =
        _listSizeLiveData

    private val _radioStationsCountLiveData = MutableLiveData<Int>()
    val radioStationsCountLiveData: LiveData<Int> =
        _radioStationsCountLiveData

    private val _serverIsDownLiveData = MutableLiveData<Boolean>()
    val serverIsDownLiveData: LiveData<Boolean> =
        _serverIsDownLiveData

    // Список, куда будут сохраняться метаданные по каждой радиостанции с помощью метода fetchMediaData()
    var radioStations = emptyList<MediaMetadataCompat>() // meta info about radioStations

    // 005 CLAUDE // Номер последней начатой загрузки: отменённая загрузка не должна менять state, если после неё уже началась следующая
    private val fetchGeneration = AtomicInteger(0)

    // Список лямбд action, которые будут передаваться в метод whenReady(), пока state == STATE_CREATED или state == STATE_INITIALIZING
    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    var isFavoriteEmpty = true // Initializer required, not a nullable type
        private set // the setter is private and has the default implementation

    // Параметр state с setter для того, чтобы можно было привязать к этому параметру определенную логику
    private var state: State = STATE_CREATED // State on default
        set(value) {
            if (value == STATE_INITIALIZED || value == STATE_ERROR) {
                synchronized(onReadyListeners) { // synchronized for save change
                    field = value // sign a new value to the field

//                    <!-- 001 claude
//                    onReadyListeners.forEach { listener ->
                    // Каждая лямбда должна сработать один раз. Раньше список не очищался, и при каждой загрузке нового плейлиста
                    // заново вызывались все старые лямбды (повторный result.sendResult() в onLoadChildren, повторный playerPrepared() со старым mediaId)
                    val listeners = onReadyListeners.toList()
                    onReadyListeners.clear()
                    listeners.forEach { listener ->
//                        001 claude -->

                        listener(state == STATE_INITIALIZED) // go through list and call needed lambda function. If there will be STATE_ERROR instead STATE_INITIALIZED, we will get "false". So we can check, if it was successful or not
                    }
                }
            } else {
                field = value // if it is STATE_CREATED or STATE_INITIALIZING
            }
        }

    // A function which will add actions to our list of actions (returns boolean - if it is ready or not)
    fun whenReady(action: (Boolean) -> Unit): Boolean {
        return if (state == STATE_CREATED || state == STATE_INITIALIZING) {
            onReadyListeners += action // We are not ready, so just add action to list (we will do it later, when we will be ready)
            false // not ready
        } else {
            action(state == STATE_INITIALIZED) // we are ready, so we can call action
            true
        }
    }

    // Метод для СОХРАНЕНИЯ МЕТАДАННЫХ по каждой радиостанции. Создаём список MediaMetadataCompat
    suspend fun fetchMediaData(countryCode: String) = withContext(Dispatchers.IO) {

        // <!-- 005 claude
        val generation = fetchGeneration.incrementAndGet()
        val startTime = SystemClock.elapsedRealtime()
        // 005 claude -->

        state = STATE_INITIALIZING
//        val allRadioStations = networkRadioDataSource.getAllRadioStationsList()

        // Получаем ответ с сервера в виде Resource с данными
        // <!-- 005 claude
//        val countryCodeRadioStationsResource =
//            networkRadioDataSource.getRadioStationList(countryCode)

        val countryCodeRadioStationsResource = try {
            // ensureActive(): если загрузку отменили, пока шёл запрос, полученный результат не применяем
            networkRadioDataSource.getRadioStationList(countryCode).also { ensureActive() }
        } catch (e: CancellationException) {
            // Загрузку отменили (выбран другой плейлист или полоса загрузки висела слишком долго). Текущий плейлист остаётся рабочим.
            // state возвращаем, только если после этой загрузки не началась новая - она сама выставит state, когда закончит
            Log.d(TAG, "PLAYLIST_UPDATE: Загрузка плейлиста $countryCode отменена, текущий плейлист не меняем")
            if (generation == fetchGeneration.get()) state = STATE_INITIALIZED
            throw e
        }
        // 005 claude -->

        // И сначала проверяем, не было ли ошибки HttpException при обращении к серверу.
        // Пустой список - тоже ошибка: NetworkRadioDataSource возвращает его, если ни один сервер не ответил
        // (на карте есть только те страны, где радиостанции есть)

        // <!-- 003 claude
//        if (countryCodeRadioStationsResource.status == Status.ERROR) {
//            _serverIsDownLiveData.call()

        if (countryCodeRadioStationsResource.status == Status.ERROR || countryCodeRadioStationsResource.data.isNullOrEmpty()) {
//            Log.d(TAG, "PLAYLIST_UPDATE: Не удалось скачать плейлист $countryCode, текущий плейлист не меняем")
            Log.d(
                TAG,
                "PLAYLIST_UPDATE: Не удалось скачать плейлист $countryCode за ${SystemClock.elapsedRealtime() - startTime} мс, текущий плейлист не меняем"
            )
            // radioStations не трогаем: плейер продолжает играть текущий плейлист, список в плейере остаётся прежним.
            // MainActivity покажет диалог и уберёт полосу загрузки
            _serverIsDownLiveData.call()
            // Раньше state оставался STATE_INITIALIZING навсегда, и лямбды whenReady() (в т.ч. из onLoadChildren) больше не вызывались
            state = STATE_INITIALIZED
            // 003 claude -->

        } else {
            countryCodeRadioStationsResource.data?.let { radioStationRemoteList ->
                // А затем уже, если такой ошибки не было, обрабатываем полученные данные

                // Отправляем цифру в MusicService для Broadcast
                val listSize = radioStationRemoteList.size
                _listSizeLiveData.postValue(listSize)
                var radioStationsCount = 0
                var percentCount = 10

                Log.d(
                    TAG,
                    "Загружаем метаданные fetchMediaData - $countryCode, listSize = $listSize. Отправляем BROADCAST"
                )
                radioStations = radioStationRemoteList.map { radioStationRemote ->

                    // Подсчёт для Broadcast
                    radioStationsCount += 1
                    val countingForBroadcast = percentCount * listSize / 100
                    if (radioStationsCount == countingForBroadcast) {
                        percentCount += 10
                        _radioStationsCountLiveData.postValue(radioStationsCount)
                    }

                    if (radioStationRemote.url_resolved.isEmpty()) {
                        Log.d(
                            TAG,
                            "Found nullable urlResolved: name = ${radioStationRemote.name}, url = ${radioStationRemote.url}, urlResolved = ${radioStationRemote.url_resolved}"
                        )
                        // Если у станции нет url_resolved, можно пойти двумя путями. Или мы вместо него сохраняем себе url, либо же пропускаем эту станцию и не сохраняем себе. Иначе возникнет ошибка в методе ниже (asMediaItems()), т.к. mediaId будет пустой
                        // Попробую убрать такие станции с помощью .filter { !it.description.mediaId.isNullOrEmpty() }
                    }

                    MediaMetadataCompat.Builder()
                        .putString(
                            METADATA_KEY_MEDIA_ID,
                            radioStationRemote.stationuuid
                        ) // media Id / stationuuid (Primary key) /
                        .putString(
                            METADATA_KEY_MEDIA_URI,
                            radioStationRemote.url_resolved
                        ) // url_resolved
                        .putString(METADATA_KEY_TITLE, radioStationRemote.name) // station name
                        .putString(
                            METADATA_KEY_DISPLAY_TITLE,
                            radioStationRemote.name
                        ) // station name
                        .putLong(
                            METADATA_KEY_DOWNLOAD_STATUS,
                            radioStationRemote.clickcount.toLong()
                        ) // click count
                        .putString(METADATA_KEY_ARTIST, radioStationRemote.country) // country
                        .putString(
                            METADATA_KEY_DISPLAY_SUBTITLE,
                            radioStationRemote.countrycode
                        ) // country code
                        .build()
                }.filter {
                    it.description.mediaUri.toString().isNotEmpty()
                }

                Log.d(TAG, "Получаем список размером ${radioStations.size}")
                _notifyChildrenChangedLiveData.call()
                state = STATE_INITIALIZED

            }
        }
    }

    // Метод для СОХРАНЕНИЯ МЕТАДАННЫХ по каждой радиостанции. Создаём список MediaMetadataCompat
    suspend fun fetchFavouriteMediaData() = withContext(Dispatchers.IO) {
        fetchGeneration.incrementAndGet() // 005 claude
        state = STATE_INITIALIZING
        val favouriteRadioStations = radioStationDAO.getFavoriteRadioStationList(true)

        // Отправляем цифру в MusicService для Broadcast
        val listSize = favouriteRadioStations.size
        _listSizeLiveData.postValue(listSize)
        var radioStationsCount = 0
        var percentCount = 10

        Log.d(
            TAG,
            "Загружаем метаданные fetchMediaData - FAV, listSize = $listSize. Отправляем BROADCAST"
        )
        if (favouriteRadioStations.isNotEmpty()) {
            isFavoriteEmpty = false

            radioStations = favouriteRadioStations.map { radioStationLocal ->

                // Подсчёт для Broadcast
                radioStationsCount += 1
                val countingForBroadcast = percentCount * listSize / 100
                if (radioStationsCount == countingForBroadcast) {
                    percentCount += 10
                    _radioStationsCountLiveData.postValue(radioStationsCount)
                }

                if (radioStationLocal.urlResolved.isEmpty()) {
                    Log.d(
                        TAG,
                        "Found nullable urlResolved: name = ${radioStationLocal.stationName}, urlResolved = ${radioStationLocal.urlResolved}"
                    )
                    // Если у станции нет url_resolved, можно пойти двумя путями. Или мы вместо него сохраняем себе url, либо же пропускаем эту станцию и не сохраняем себе. Иначе возникнет ошибка в методе ниже (asMediaItems()), т.к. mediaId будет пустой
                    // Попробую убрать такие станции с помощью .filter { !it.description.mediaId.isNullOrEmpty() }
                }

                MediaMetadataCompat.Builder()
                    .putString(
                        METADATA_KEY_MEDIA_ID,
                        radioStationLocal.stationuuid
                    ) // media Id / stationuuid (Primary key)
                    .putString(
                        METADATA_KEY_MEDIA_URI,
                        radioStationLocal.urlResolved
                    ) // url_resolved
                    .putString(METADATA_KEY_TITLE, radioStationLocal.stationName) // station name
                    .putString(
                        METADATA_KEY_DISPLAY_TITLE,
                        radioStationLocal.stationName
                    ) // station name
                    .putLong(
                        METADATA_KEY_DOWNLOAD_STATUS,
                        radioStationLocal.clickCount.toLong()
                    ) // click count
                    .putString(METADATA_KEY_ARTIST, radioStationLocal.country) // country
                    .putString(
                        METADATA_KEY_DISPLAY_SUBTITLE,
                        radioStationLocal.countryCode + "_FAV"
                    ) // country code
                    .build()
            }.filter {
                it.description.mediaUri.toString().isNotEmpty()
            }

//            state = STATE_INITIALIZED
        } else {
            isFavoriteEmpty = true
        }

//        radioStations = favouriteRadioStations.map { radioStationLocal ->
//            MediaMetadataCompat.Builder()
//                .putString(
//                    METADATA_KEY_MEDIA_ID,
//                    radioStationLocal.url
//                ) // media Id / url (Primary key)
//                .putString(METADATA_KEY_MEDIA_URI, radioStationLocal.urlResolved) // url_resolved
//                .putString(METADATA_KEY_TITLE, radioStationLocal.stationName) // station name
//                .putString(
//                    METADATA_KEY_DISPLAY_TITLE,
//                    radioStationLocal.stationName
//                ) // station name
//                .putLong(
//                    METADATA_KEY_DOWNLOAD_STATUS,
//                    radioStationLocal.clickCount.toLong()
//                ) // click count
//                .putString(METADATA_KEY_ARTIST, radioStationLocal.country) // country
//                .putString(
//                    METADATA_KEY_DISPLAY_SUBTITLE,
//                    radioStationLocal.countryCode + "_FAV"
//                ) // country code
//                .build()
//        }

        Log.d(TAG, "Получаем список размером ${radioStations.size}")
        _notifyChildrenChangedLiveData.call()
        state = STATE_INITIALIZED
    }

    // A list of media items. Список MediaMetadataCompat теперь преобразуем в список MediaBrowserCompat.MediaItem (для нашей MainViewModel). Сформированный список вернется как результат работы функции там, где её вызвали.
    // Метод необходимо вызывать после того, как список radioStations будет полностью сформирован!
    fun asMediaItems() = radioStations.map { radioStation ->
        val extrasRadioStationInfo = Bundle().apply {
            putLong("ClickCount", radioStation.getLong(METADATA_KEY_DOWNLOAD_STATUS))
            putString("Country", radioStation.getString(METADATA_KEY_ARTIST))
        }

        if (radioStation.description.mediaUri.toString().isEmpty()) {
            Log.d(
                TAG,
                "name = ${radioStation.description.title}, url = ${radioStation.description.mediaUri}, mediaUri = ${radioStation.description.mediaUri}"
            )
        }

        val desc = MediaDescriptionCompat.Builder()
            .setMediaId(radioStation.description.mediaId) // media Id / stationuuid (Primary key)
            .setMediaUri(
                radioStation.getString(METADATA_KEY_MEDIA_URI).toUri()
            ) // url_resolved
            .setTitle(radioStation.description.title) // station name
            .setSubtitle(radioStation.description.subtitle) // country code
            .setExtras(extrasRadioStationInfo) // <- click count, country in extras
            .build()
        MediaBrowserCompat.MediaItem(desc, FLAG_PLAYABLE)
    }
        .toMutableList() // Flag FLAG_PLAYABLE indicates that the item is playable, not the item that has children of its own.

    // DefaultDataSourceFactory is deprecated
//    fun asMediaSource(dataSourceFactory: DefaultDataSourceFactory): ConcatenatingMediaSource {
//        val concatenatingMediaSource = ConcatenatingMediaSource() // empty by default
//        radioStations.forEach { radioStation ->
//            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
//                .createMediaSource(radioStation.getString(METADATA_KEY_MEDIA_URI).toUri())
//            concatenatingMediaSource.addMediaSource(mediaSource) // Add one by one to our concatenatingMediaSource
//        }
//        return concatenatingMediaSource
//    }

//    fun asMediaSource(dataSourceFactory: DefaultDataSource.Factory): ConcatenatingMediaSource {
//        val concatenatingMediaSource = ConcatenatingMediaSource() // empty by default
//        radioStations.forEach { radioStation ->
//            val mediaItem =
//                MediaItem.fromUri(radioStation.getString(METADATA_KEY_MEDIA_URI).toUri())
//            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
//                .createMediaSource(mediaItem)
//            concatenatingMediaSource.addMediaSource(mediaSource) // Add one by one to our concatenatingMediaSource
//        }
//        return concatenatingMediaSource
//    }
//
//    fun asHlsMediaSource(httpDataSourceFactory: DefaultHttpDataSource.Factory): ConcatenatingMediaSource {
//        val concatenatingMediaSource = ConcatenatingMediaSource() // empty by default
//        radioStations.forEach { radioStation ->
//            val mediaItem =
//                MediaItem.fromUri(radioStation.getString(METADATA_KEY_MEDIA_URI).toUri())
//            val mediaSource = HlsMediaSource.Factory(httpDataSourceFactory)
//                .createMediaSource(mediaItem)
//            concatenatingMediaSource.addMediaSource(mediaSource) // Add one by one to our concatenatingMediaSource
//        }
//        return concatenatingMediaSource
//    }

    // Для формирования плейлиста из нескольких песен/радиостанций. Info for exoplayer to stream songs
    fun asMediaSourcePlaylist(
        playlist: List<MediaMetadataCompat>, // 001 claude
        httpDataSourceFactory: DefaultHttpDataSource.Factory,
        dataSourceFactory: DefaultDataSource.Factory
    ): ConcatenatingMediaSource {

//        <!-- 001 claude
//        val concatenatingMediaSource = ConcatenatingMediaSource() // empty by default
//        radioStations.forEach { radioStation ->
        // useLazyPreparation = true: источник станции готовится только когда до неё доходит очередь.
        // По умолчанию (ConcatenatingMediaSource()) сразу готовятся ВСЕ станции плейлиста (до 500), и каждая HLS (.m3u8) станция
        // постоянно перезагружает свой live-плейлист -> Timeline постоянно меняется -> MediaSessionConnector постоянно
        // пересылает метаданные (onMetadataChanged) + лишний интернет-трафик
        val concatenatingMediaSource = ConcatenatingMediaSource(
            /* isAtomic = */ false,
            /* useLazyPreparation = */ true,
            ShuffleOrder.DefaultShuffleOrder(0)
        ) // empty by default
        playlist.forEach { radioStation ->
//            001 claude 001 -->

            val mediaUri = radioStation.description.mediaUri.toString()

            Log.d(
                TAG,
                "PLAYLIST_UPDATE: 5.$TAG, asMediaSourceTest(). Проверяем, заканчивается ли ссылка на .m3u8. Формируем данные для плейлиста"
            )
            if (mediaUri.endsWith(".m3u8")
            ) {
                // .m3u8 -> .asHlsMediaSource(httpDataSourceFactory)
                val mediaItem =
                    MediaItem.fromUri(radioStation.getString(METADATA_KEY_MEDIA_URI).toUri())
                val mediaSource = HlsMediaSource.Factory(httpDataSourceFactory)
                    .createMediaSource(mediaItem)
                concatenatingMediaSource.addMediaSource(mediaSource) // Add one by one to our concatenatingMediaSource
            } else {
                // else -> .asMediaSource(dataSourceFactory)
                val mediaItem =
                    MediaItem.fromUri(radioStation.getString(METADATA_KEY_MEDIA_URI).toUri())
                val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
                concatenatingMediaSource.addMediaSource(mediaSource) // Add one by one to our concatenatingMediaSource
            }

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