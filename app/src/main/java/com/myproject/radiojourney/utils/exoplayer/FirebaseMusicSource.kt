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
    private val radioStationDAO: IRadioStationDAO,
    // Прогресс загрузки и "сервер недоступен" для экрана (раньше - LiveData, которые MusicService пересылал бродкастами)
    private val playlistDownloadStatus: PlaylistDownloadStatus
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

    // Список, куда будут сохраняться метаданные по каждой радиостанции с помощью метода fetchMediaData()
    // @Volatile: список записывается в потоке IO, а читается в главном - без @Volatile главный поток может увидеть старое значение
    @Volatile
    var radioStations =
        emptyList<MediaItem>() // meta info about radioStations (media3 MediaItem: адрес потока + метаданные)
        private set

    // Картинка для уведомления
    private val artworkUri =
        "android.resource://${context.packageName}/drawable/radio_heissenstein_pixabay".toUri()

    // Номер последней начатой загрузки: отменённая загрузка не должна менять state, если после неё уже началась следующая
    private val fetchGeneration = AtomicInteger(0)

    // Состояние загрузки плейлиста и ожидающие её действия (общий класс, см. ReadinessState)
    private val readiness = ReadinessState()

    fun whenReady(action: (Boolean) -> Unit): Boolean = readiness.whenReady(action)

    // Одна станция перед преобразованием в MediaItem (из сервера или из избранного в Room)
    private class StationData(
        val stationUuid: String,
        val urlResolved: String,
        val stationName: String,
        val clickCount: Long,
        val country: String,
        val countryCode: String // для плейлиста избранного - с суффиксом "_FAV"
    )

    // Загрузить плейлист страны с сервера
    suspend fun fetchMediaData(countryCode: String) = withContext(Dispatchers.IO) {
        val generation = fetchGeneration.incrementAndGet()
        val startTime = SystemClock.elapsedRealtime()
        readiness.state = STATE_INITIALIZING

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
            if (generation == fetchGeneration.get()) readiness.state = STATE_INITIALIZED
            throw e
        }

        val radioStationRemoteList = countryCodeRadioStationsResource.data
        // Пустой список - тоже ошибка: NetworkRadioDataSource возвращает его, если ни один сервер не ответил
        // (на карте есть только те страны, где радиостанции есть)
        if (countryCodeRadioStationsResource.status == Status.ERROR || radioStationRemoteList.isNullOrEmpty()) {
            Log.d(
                TAG,
                "PLAYLIST_UPDATE: Не удалось скачать плейлист $countryCode за ${SystemClock.elapsedRealtime() - startTime} мс, текущий плейлист не меняем"
            )
            // radioStations не трогаем: плейер продолжает играть текущий плейлист, список в плейере остаётся прежним.
            // MainActivity покажет диалог и уберёт полосу загрузки
            playlistDownloadStatus.notifyServerIsDown()
            readiness.state = STATE_INITIALIZED
            return@withContext
        }

        Log.d(
            TAG,
            "Загружаем метаданные fetchMediaData - $countryCode, listSize = ${radioStationRemoteList.size}"
        )
        setPlaylist(radioStationRemoteList.map { remote ->
            StationData(
                stationUuid = remote.stationuuid, // media Id / stationuuid (Primary key)
                urlResolved = remote.url_resolved,
                stationName = remote.name,
                clickCount = remote.clickcount.toLong(),
                country = remote.country,
                countryCode = remote.countrycode
            )
        })
    }

    // Загрузить плейлист избранного из Room
    suspend fun fetchFavouriteMediaData() = withContext(Dispatchers.IO) {
        fetchGeneration.incrementAndGet()
        readiness.state = STATE_INITIALIZING
        val favouriteRadioStations = radioStationDAO.getFavoriteRadioStationList(true)
        Log.d(
            TAG,
            "Загружаем метаданные fetchMediaData - FAV, listSize = ${favouriteRadioStations.size}"
        )

        if (favouriteRadioStations.isEmpty()) {
            // Избранное пустое - текущий плейлист не меняем
            _notifyChildrenChangedLiveData.call()
            readiness.state = STATE_INITIALIZED
            return@withContext
        }

        setPlaylist(favouriteRadioStations.map { local ->
            StationData(
                stationUuid = local.stationuuid,
                urlResolved = local.urlResolved,
                stationName = local.stationName,
                clickCount = local.clickCount.toLong(),
                country = local.country,
                countryCode = local.countryCode + "_FAV"
            )
        })
    }

    // Общая часть обеих загрузок (раньше повторялась в fetchMediaData и fetchFavouriteMediaData):
    // преобразование в MediaItem с прогрессом каждые 10% и сообщение экрану, что плейлист готов
    private fun setPlaylist(stations: List<StationData>) {
        val listSize = stations.size
        var percentCount = 10
        playlistDownloadStatus.resetProgress()

        radioStations = stations.mapIndexed { index, station ->
            val radioStationsCount = index + 1
            if (radioStationsCount == percentCount * listSize / 100) {
                percentCount += 10
                playlistDownloadStatus.setProgress(radioStationsCount, listSize)
            }
            toRadioStationMediaItem(station)
        }.filter {
            // Станции без адреса потока (url_resolved) играть нельзя - пропускаем
            val hasUrl = it.localConfiguration?.uri.toString().isNotEmpty()
            if (!hasUrl) Log.d(TAG, "Found empty urlResolved: name = ${it.mediaMetadata.title}")
            hasUrl
        }

        Log.d(TAG, "Получаем список размером ${radioStations.size}")
        _notifyChildrenChangedLiveData.call()
        readiness.state = STATE_INITIALIZED
    }

    // Станция в формате media3 - один объект и для плеера, и для уведомления, и для экрана
    private fun toRadioStationMediaItem(station: StationData): MediaItem {
        // Адрес потока (localConfiguration) media3 не передаёт из сервиса на экран (MediaBrowser),
        // поэтому для экрана дублируем его в extras. Там же - число прослушиваний и страна
        val extras = Bundle().apply {
            putString(EXTRA_URL_RESOLVED, station.urlResolved)
            putLong(EXTRA_CLICK_COUNT, station.clickCount)
            putString(EXTRA_COUNTRY, station.country)
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(station.stationName) // station name
            .setDisplayTitle(station.stationName)
            .setArtist(notificationCountryText(station.countryCode)) // вторая строка уведомления: страна или "Избранное: страна"
            .setSubtitle(station.countryCode) // country code ("PL" или "PL_FAV")
            .setArtworkUri(artworkUri) // большая картинка в уведомлении
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            .setExtras(extras)
            .build()

        return MediaItem.Builder()
            .setMediaId(station.stationUuid)
            .setUri(station.urlResolved)
            // HLS (.m3u8) воспроизводится другим источником - DefaultMediaSourceFactory выберет его по MIME-типу
            .apply { if (station.urlResolved.endsWith(".m3u8")) setMimeType(MimeTypes.APPLICATION_M3U8) }
            .setMediaMetadata(metadata)
            .build()
    }

    private fun notificationCountryText(countryCode: String): String {
        val isFavourite = countryCode.endsWith("_FAV")
        val countryName = Locale("", countryCode.removeSuffix("_FAV")).displayName
        return if (isFavourite) context.getString(R.string.homeRadio_goToFavourites) + ": " + countryName else countryName
    }
}