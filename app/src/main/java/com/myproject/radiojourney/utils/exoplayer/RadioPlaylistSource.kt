package com.myproject.radiojourney.utils.exoplayer

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import com.myproject.radiojourney.R
import com.myproject.radiojourney.domain.iRepository.IFavoriteStationRepository
import com.myproject.radiojourney.domain.iRepository.IMainRadioStationRepository
import com.myproject.radiojourney.domain.iRepository.IMyStationRepository
import com.myproject.radiojourney.domain.model.RadioStation
import com.myproject.radiojourney.other.Constants.FAVOURITES_COUNTRY_CODE_SUFFIX
import com.myproject.radiojourney.other.Constants.MY_STATIONS_COUNTRY_CODE
import com.myproject.radiojourney.other.ServerError
import com.myproject.radiojourney.other.Status
import com.myproject.radiojourney.utils.exoplayer.State.STATE_INITIALIZED
import com.myproject.radiojourney.utils.exoplayer.State.STATE_INITIALIZING
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

/**
 * Плейлист плеера: загрузка станций страны или избранного и преобразование в MediaItem.
 *
 * Раньше назывался FirebaseMusicSource (название осталось от учебного проекта, Firebase здесь не используется)
 * и ходил напрямую в NetworkRadioDataSource и в DAO Room. Теперь работает через интерфейсы репозиториев из domain,
 * как и остальные части приложения
 */
class RadioPlaylistSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mainRadioStationRepository: IMainRadioStationRepository,
    private val favoriteStationRepository: IFavoriteStationRepository,
    private val myStationRepository: IMyStationRepository,
    // Прогресс загрузки и "сервер недоступен" для экрана
    private val playlistDownloadStatus: PlaylistDownloadStatus
) {
    companion object {
        private const val TAG = "RadioPlaylistSource"
    }

    // Скачан новый плейлист - MusicService сообщает экрану (notifyChildrenChanged). Раньше - LiveData с observeForever в сервисе
    private val _playlistChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val playlistChanged: SharedFlow<Unit> = _playlistChanged.asSharedFlow()

    // Станции текущего плейлиста.
    // @Volatile: список записывается в потоке IO, а читается в главном - без @Volatile главный поток может увидеть старое значение
    @Volatile
    var radioStations = emptyList<MediaItem>()
        private set

    // Картинка для уведомления
    private val artworkUri =
        "android.resource://${context.packageName}/drawable/radio_heissenstein_pixabay".toUri()

    // Номер последней начатой загрузки: отменённая загрузка не должна менять state, если после неё уже началась следующая
    private val fetchGeneration = AtomicInteger(0)

    // Состояние загрузки плейлиста и ожидающие её действия (общий класс, см. ReadinessState)
    private val readiness = ReadinessState()

    fun whenReady(action: (Boolean) -> Unit): Boolean = readiness.whenReady(action)

    // Загрузить плейлист страны с сервера
    suspend fun fetchMediaData(countryCode: String) = withContext(Dispatchers.IO) {
        val generation = fetchGeneration.incrementAndGet()
        val startTime = SystemClock.elapsedRealtime()
        readiness.state = STATE_INITIALIZING

        val radioStationsResource = try {
            // ensureActive(): если загрузку отменили, пока шёл запрос, полученный результат не применяем
            mainRadioStationRepository.getRadioStationList(countryCode).also { ensureActive() }
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

        val radioStationList = radioStationsResource.data?.stations
        // Пустой список - тоже ошибка (на карте есть только те страны, где радиостанции есть)
        if (radioStationsResource.status == Status.ERROR || radioStationList.isNullOrEmpty()) {
            Log.d(
                TAG,
                "PLAYLIST_UPDATE: Не удалось скачать плейлист $countryCode за ${SystemClock.elapsedRealtime() - startTime} мс, текущий плейлист не меняем"
            )
            // radioStations не трогаем: плейер продолжает играть текущий плейлист, список в плейере остаётся прежним.
            // MainActivity покажет диалог и уберёт полосу загрузки
            playlistDownloadStatus.notifyServerIsDown(ServerError.fromMessage(radioStationsResource.message))
            readiness.state = STATE_INITIALIZED
            return@withContext
        }

        Log.d(
            TAG,
            "Загружаем метаданные fetchMediaData - $countryCode, listSize = ${radioStationList.size}"
        )
        setPlaylist(radioStationList)

        // Плейлист запасной (сохранённый или только популярные станции) - экран скажет об этом пользователю
        radioStationsResource.data?.takeIf { it.isFallback }
            ?.let { playlistDownloadStatus.notifyFallbackPlaylistUsed(it) }
    }

    // Загрузить плейлист избранного из Room
    suspend fun fetchFavouriteMediaData() = withContext(Dispatchers.IO) {
        fetchGeneration.incrementAndGet()
        readiness.state = STATE_INITIALIZING
        val favouriteRadioStations = favoriteStationRepository.getFavoriteRadioStationList()
        Log.d(
            TAG,
            "Загружаем метаданные fetchMediaData - FAV, listSize = ${favouriteRadioStations.size}"
        )

        if (favouriteRadioStations.isEmpty()) {
            // Избранное пустое - текущий плейлист не меняем
            _playlistChanged.tryEmit(Unit)
            readiness.state = STATE_INITIALIZED
            return@withContext
        }

        // У станций плейлиста избранного код страны с суффиксом "_FAV" - так плеер отличает этот плейлист
        setPlaylist(favouriteRadioStations.map { it.copy(countryCode = it.countryCode + FAVOURITES_COUNTRY_CODE_SUFFIX) })
    }

    // Загрузить плейлист своих станций из Room ("Мои радиостанции"). Сети здесь нет: станции добавил сам пользователь,
    // и у них уже стоит код страны MY_STATIONS_COUNTRY_CODE (см. DataMappers)
    suspend fun fetchMyStationsMediaData() = withContext(Dispatchers.IO) {
        fetchGeneration.incrementAndGet()
        readiness.state = STATE_INITIALIZING
        val myStations = myStationRepository.getMyStationListOnce()
        Log.d(TAG, "Загружаем метаданные fetchMediaData - MY, listSize = ${myStations.size}")

        if (myStations.isEmpty()) {
            // Своих станций пока нет - текущий плейлист не меняем
            _playlistChanged.tryEmit(Unit)
            readiness.state = STATE_INITIALIZED
            return@withContext
        }

        setPlaylist(myStations)
    }

    // Общая часть обеих загрузок: преобразование в MediaItem с прогрессом каждые 10% и сообщение экрану, что плейлист готов
    private fun setPlaylist(stations: List<RadioStation>) {
        val listSize = stations.size
        var percentCount = 10
        playlistDownloadStatus.resetProgress()

        radioStations = stations.mapIndexed { index, station ->
            val radioStationsCount = index + 1
            if (radioStationsCount == percentCount * listSize / 100) {
                percentCount += 10
                playlistDownloadStatus.setProgress(radioStationsCount, listSize)
            }
            station.toMediaItem(notificationCountryText(station.countryCode), artworkUri)
        }.filter {
            // Станции без адреса потока (url_resolved) играть нельзя - пропускаем
            val hasUrl = it.localConfiguration?.uri.toString().isNotEmpty()
            if (!hasUrl) Log.d(TAG, "Found empty urlResolved: name = ${it.mediaMetadata.title}")
            hasUrl
        }

        Log.d(TAG, "Получаем список размером ${radioStations.size}")
        _playlistChanged.tryEmit(Unit)
        readiness.state = STATE_INITIALIZED
    }

    // Вторая строка уведомления: страна, "Избранное: страна" или "Мои радиостанции"
    private fun notificationCountryText(countryCode: String): String {
        // У своих станций страны нет, а Locale("", "MY").displayName вернул бы "Малайзия" - поэтому отдельная ветка
        if (countryCode == MY_STATIONS_COUNTRY_CODE) return context.getString(R.string.myStations_title)

        val isFavourite = countryCode.endsWith(FAVOURITES_COUNTRY_CODE_SUFFIX)
        val countryName =
            Locale("", countryCode.removeSuffix(FAVOURITES_COUNTRY_CODE_SUFFIX)).displayName
        return if (isFavourite) context.getString(R.string.homeRadio_goToFavourites) + ": " + countryName else countryName
    }
}