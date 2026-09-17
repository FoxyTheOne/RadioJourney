package com.myproject.radiojourney.utils.exoplayer

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import com.myproject.radiojourney.R
import com.myproject.radiojourney.data.dataSource.network.INetworkRadioDataSource
import com.myproject.radiojourney.data.localDatabaseRoom.IRadioStationDAO
import com.myproject.radiojourney.other.Status
import com.myproject.radiojourney.utils.exoplayer.State.STATE_CREATED
import com.myproject.radiojourney.utils.exoplayer.State.STATE_ERROR
import com.myproject.radiojourney.utils.exoplayer.State.STATE_INITIALIZED
import com.myproject.radiojourney.utils.exoplayer.State.STATE_INITIALIZING
import com.myproject.radiojourney.utils.extension.call
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

// We need time to upload music from firebase or other data
class FirebaseMusicSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkRadioDataSource: INetworkRadioDataSource,
    private val radioStationDAO: IRadioStationDAO
) {
    companion object {
        private const val TAG = "FirebaseMusicSource"

        // Ключи extras станции (MediaMetadata.extras). Читаются в MainRadioUseCase
        const val EXTRA_URL_RESOLVED = "UrlResolved"
        const val EXTRA_CLICK_COUNT = "ClickCount"
        const val EXTRA_COUNTRY = "Country"
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
    var radioStations =
        emptyList<MediaItem>() // meta info about radioStations (media3 MediaItem: адрес потока + метаданные)

    // Картинка для уведомления (раньше MusicNotificationManager.getCurrentLargeIcon)
    private val artworkUri =
        "android.resource://${context.packageName}/drawable/radio_heissenstein_pixabay".toUri()

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
            Log.d(
                TAG,
                "PLAYLIST_UPDATE: Загрузка плейлиста $countryCode отменена, текущий плейлист не меняем"
            )
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

                    toRadioStationMediaItem(
                        stationUuid = radioStationRemote.stationuuid, // media Id / stationuuid (Primary key)
                        urlResolved = radioStationRemote.url_resolved,
                        stationName = radioStationRemote.name,
                        clickCount = radioStationRemote.clickcount.toLong(),
                        country = radioStationRemote.country,
                        countryCode = radioStationRemote.countrycode
                    )
                }.filter {
                    it.localConfiguration?.uri.toString().isNotEmpty()
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

                toRadioStationMediaItem(
                    stationUuid = radioStationLocal.stationuuid, // media Id / stationuuid (Primary key)
                    urlResolved = radioStationLocal.urlResolved,
                    stationName = radioStationLocal.stationName,
                    clickCount = radioStationLocal.clickCount.toLong(),
                    country = radioStationLocal.country,
                    countryCode = radioStationLocal.countryCode + "_FAV"
                )
            }.filter {
                it.localConfiguration?.uri.toString().isNotEmpty()
            }

//            state = STATE_INITIALIZED
        } else {
            isFavoriteEmpty = true
        }

        Log.d(TAG, "Получаем список размером ${radioStations.size}")
        _notifyChildrenChangedLiveData.call()
        state = STATE_INITIALIZED
    }

    // A list of media items. Список MediaMetadataCompat теперь преобразуем в список MediaBrowserCompat.MediaItem (для нашей MainViewModel). Сформированный список вернется как результат работы функции там, где её вызвали.
    // Метод необходимо вызывать после того, как список radioStations будет полностью сформирован!
    // Список станций для экрана (MainViewModel получает его через MediaBrowser.getChildren()).
    // С media3 radioStations - уже готовые MediaItem, отдельно преобразовывать не нужно
    fun asMediaItems(): List<MediaItem> = radioStations

    // Станция в формате media3 - один объект и для плеера, и для уведомления, и для экрана.
    // Раньше были MediaMetadataCompat (метаданные) + MediaBrowserCompat.MediaItem (экран) + ConcatenatingMediaSource (плеер)
    private fun toRadioStationMediaItem(
        stationUuid: String,
        urlResolved: String,
        stationName: String,
        clickCount: Long,
        country: String,
        countryCode: String // для плейлиста избранного - с суффиксом "_FAV"
    ): MediaItem {
        // Адрес потока (localConfiguration) media3 не передаёт из сервиса на экран (MediaBrowser),
        // поэтому для экрана дублируем его в extras. Там же - число прослушиваний и страна
        val extras = Bundle().apply {
            putString(EXTRA_URL_RESOLVED, urlResolved)
            putLong(EXTRA_CLICK_COUNT, clickCount)
            putString(EXTRA_COUNTRY, country)
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(stationName) // station name
            .setDisplayTitle(stationName)
            .setArtist(notificationCountryText(countryCode)) // вторая строка уведомления: страна или "Избранное: страна"
            .setSubtitle(countryCode) // country code ("PL" или "PL_FAV")
            .setArtworkUri(artworkUri) // большая картинка в уведомлении
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            .setExtras(extras)
            .build()

        return MediaItem.Builder()
            .setMediaId(stationUuid)
            .setUri(urlResolved)
            // HLS (.m3u8) воспроизводится другим источником - DefaultMediaSourceFactory выберет его по MIME-типу
            .apply { if (urlResolved.endsWith(".m3u8")) setMimeType(MimeTypes.APPLICATION_M3U8) }
            .setMediaMetadata(metadata)
            .build()
    }

    // Раньше этот текст формировал MusicNotificationManager.getCurrentContentText()
    private fun notificationCountryText(countryCode: String): String {
        val isFavourite = countryCode.endsWith("_FAV")
        val countryName = Locale("", countryCode.removeSuffix("_FAV")).displayName
        return if (isFavourite) context.getString(R.string.homeRadio_goToFavourites) + ": " + countryName else countryName
    }
}

enum class State {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}