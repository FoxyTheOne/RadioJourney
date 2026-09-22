package com.myproject.radiojourney.presentation

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.google.android.gms.maps.model.CameraPosition
import com.myproject.radiojourney.R
import com.myproject.radiojourney.data.worker.CountryCacheScheduler
import com.myproject.radiojourney.domain.changeFavouriteUseCase.IChangeFavouriteUseCase
import com.myproject.radiojourney.domain.firstScreenLoadingUseCase.ILoginScreenUseCase
import com.myproject.radiojourney.domain.mainRadioUseCase.IMainRadioUseCase
import com.myproject.radiojourney.domain.model.RadioStationList
import com.myproject.radiojourney.other.Constants.ADD_SONGS
import com.myproject.radiojourney.other.Constants.CANCEL_PLAYLIST_DOWNLOAD
import com.myproject.radiojourney.other.Constants.COUNTRY_CODE_ID
import com.myproject.radiojourney.other.Constants.MEDIA_ROOT_ID
import com.myproject.radiojourney.other.Constants.PROGRESS_TIMEOUT
import com.myproject.radiojourney.other.ServerError
import com.myproject.radiojourney.presentation.model.RadioStationPresentation
import com.myproject.radiojourney.presentation.model.toDomain
import com.myproject.radiojourney.presentation.model.toPresentation
import com.myproject.radiojourney.utils.exoplayer.MusicServiceConnection
import com.myproject.radiojourney.utils.exoplayer.PlaybackStateInfo
import com.myproject.radiojourney.utils.exoplayer.PlaylistDownloadStatus
import com.myproject.radiojourney.utils.exoplayer.toRadioStation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Основная ViewModel (привязана к MainActivity): плейер в нижней панели, плейлист, полосы загрузки, избранное.
 *
 * Состояние экрана - StateFlow, одноразовые события (ошибка, "переключи ViewPager") - Channel / SharedFlow.
 * Раньше это были LiveData с обёрткой Event и LiveData, в которые "стреляли" значением true (call()).
 * developer.android.com рекомендует для новых экранов именно StateFlow: это часть Kotlin coroutines, а не Android,
 * и его удобно объединять и преобразовывать операторами Flow
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val musicServiceConnection: MusicServiceConnection,
    private val mainRadioInteractor: IMainRadioUseCase,
    private val changeFavouriteInteractor: IChangeFavouriteUseCase,
    private val loginScreenInteractor: ILoginScreenUseCase,
    playlistDownloadStatus: PlaylistDownloadStatus,
    countryCacheScheduler: CountryCacheScheduler
) : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    // Полоса загрузки, перекрывающая экран. Раньше - три LiveData: setNonClickableDp, setNonClickableCRSt и setClickable
    enum class LoadingState { NONE, DOWNLOADING_PLAYLIST, CONNECTING_STATION }

    // Станции текущего плейлиста. version растёт с каждой доставкой из сервиса: StateFlow не повторяет одинаковые значения,
    // а экрану нужно узнать и о повторной доставке того же списка (при запуске он приходит дважды)
    data class Playlist(val stations: List<RadioStationPresentation>, val version: Int)

    // Станция добавлена в избранное или убрана из него - где бы ни нажали звезду (плейер, список избранного)
    data class FavouriteChange(val station: RadioStationPresentation, val isFavourite: Boolean)

    // Прогресс загрузки плейлиста (0..100) и ошибка "сервер недоступен" - из сервиса плеера
    val playlistDownloadProgress: StateFlow<Int> = playlistDownloadStatus.progressPercent
    val serverIsDown: SharedFlow<ServerError> = playlistDownloadStatus.serverIsDown

    // Плейлист запасной: сохранённый при прошлом скачивании или только самые популярные станции
    val fallbackPlaylistUsed: SharedFlow<RadioStationList> =
        playlistDownloadStatus.fallbackPlaylistUsed

    // null - плейлист ещё загружается
    private val _playlist = MutableStateFlow<Playlist?>(null)
    val playlist: StateFlow<Playlist?> = _playlist.asStateFlow()
    val currentPlaylistStations: List<RadioStationPresentation>
        get() = _playlist.value?.stations.orEmpty()

    // Нужно вызывать playOrToggleSong, когда у нас новый плейлист, а станция была на паузе. И в то же время не нужно autoplay сразу при запуске программы
    private val _isNotJustLaunched = MutableStateFlow(false)
    val isNotJustLaunched: StateFlow<Boolean> = _isNotJustLaunched.asStateFlow()

    // SharedFlow без повтора: событие получают только те, кто подписан в момент изменения. Раньше LiveData повторяла
    // последнее значение новому подписчику, и приходилось пропускать старые события по id
    private val _favouriteChanges = MutableSharedFlow<FavouriteChange>(extraBufferCapacity = 8)
    val favouriteChanges: SharedFlow<FavouriteChange> = _favouriteChanges.asSharedFlow()

    // Иногда сбивается и в уведомлении показывает правильную станцию, а в плейере - нет. Страховка: MainActivity ещё раз переключает ViewPager
    private val _switchViewPagerOnceAgain = Channel<RadioStationPresentation>(Channel.CONFLATED)
    val switchViewPagerOnceAgain: Flow<RadioStationPresentation> =
        _switchViewPagerOnceAgain.receiveAsFlow()

    // Состояние плеера и станция в плеере
    val playbackState: StateFlow<PlaybackStateInfo?> = musicServiceConnection.playbackState
    val curPlayingSong: StateFlow<MediaItem?> = musicServiceConnection.curPlayingSong

    // Сообщения об ошибках: подключение к сервису, сеть, долгая загрузка
    private val _errorMessages = Channel<String>(Channel.BUFFERED)
    val errorMessages: Flow<String> =
        merge(_errorMessages.receiveAsFlow(), musicServiceConnection.errorMessages)

    private val _loadingState = MutableStateFlow(LoadingState.NONE)
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()

    var isServerDown = false
        private set

    private var progressTimeoutJob: Job? = null

    // Время (SystemClock.elapsedRealtime), когда показали полосу "Connecting to radio station". null - сейчас не показана
    var connectingProgressShownAt: Long? = null
        private set

    // Позиция карты на главном экране. HomeRadioFragment создаётся заново при каждом возвращении на него,
    // поэтому храним позицию здесь (MainViewModel живёт, пока открыта MainActivity)
    var mapCameraPosition: CameraPosition? = null

    // Плейлист хотя бы раз показан на экране (в ViewPager). Ожидающие этого действия хранятся в MainActivity:
    // они ссылаются на Activity, а ViewModel живёт дольше неё
    var isPlaylistReady = false
        private set

    // Получатель списка станций от сервиса (MusicServiceConnection.subscribe). Отдельное поле - чтобы в onCleared()
    // отписать именно его: MusicServiceConnection один на всё приложение, а MainViewModel создаётся заново вместе с MainActivity
    private val onChildrenLoaded: (List<MediaItem>) -> Unit = { children ->
        viewModelScope.launch {
            // Данные подтягиваются из MusicLibrarySessionCallback.onGetChildren() в MusicService.
            // MediaItem (media3) -> станция domain -> признак избранного из Room -> станция для экрана
            val radioStations =
                mainRadioInteractor.withFavouriteFlags(children.map { it.toRadioStation() })
            val version = (_playlist.value?.version ?: 0) + 1
            _playlist.value = Playlist(radioStations.map { it.toPresentation() }, version)
            Log.d(TAG, "PLAYLIST_UPDATE: 3.$TAG, onChildrenLoaded(). Данные загружены")
        }
    }
//    onChildrenLoaded - Это функция, которую MainViewModel отдаёт MusicServiceConnection. Сервис вызывает её каждый раз, когда список станций в плеере изменился — плеер прислал MediaItem'ы, а экрану нужны станции:
//    MediaItem (media3) → toRadioStation() → домен → withFavouriteFlags() (звёзды из Room) → toPresentation() → экран
//    version — это счётчик, по которому MainActivity понимает, новый это плейлист или тот же самый с изменённой звездой.
//    Проблема, которую он решает: _playlist — это StateFlow, и он отдаёт одно и то же значение в двух очень разных случаях:
//    пришёл новый плейлист (пользователь выбрал другую страну) — надо заполнить ViewPager, выбрать стартовую станцию и включить её;
//    у станции просто изменилась звезда (notifyFavouriteChanged создаёт новый список с изменённым элементом) — надо только обновить список в адаптере, ничего не переключая.
//    Отличить их по содержимому нельзя. Поэтому при загрузке нового плейлиста версия увеличивается на единицу, а при смене звезды остаётся прежней (playlist.copy(stations = ...) сохраняет номер).
//    MainActivity хранит у себя номер уже показанной версии: совпал — просто отдаёт список адаптеру, не совпал — выполняет всю логику с переключением станции. Без этого счётчика нажатие на звезду перебрасывало бы плеер на стартовую станцию плейлиста.

    init {
        // Загрузка списка стран для карты. MainViewModel создаётся один раз за запуск приложения (переживает пересоздание Activity),
        // поэтому загрузка не повторяется при смене темы или языка, как было с запуском сервиса в MainActivity.onCreate
        countryCacheScheduler.start()

        // Список радиостанций после загрузки плейлиста - для обновления UI (например, ViewPager)
        musicServiceConnection.subscribe(MEDIA_ROOT_ID, onChildrenLoaded)
    }

    // Стартовый экран: пользователь уже входил - открываем карту, нет - экран загрузки и входа.
    // null, пока значение не прочитано из хранилища (DataStore читает файл в фоновом потоке)
    val startDestinationId: StateFlow<Int?> = loginScreenInteractor.isLoggedIn()
        .map { isLoggedIn -> if (isLoggedIn) R.id.homeRadioFragment else R.id.firstScreenLoadingFragment }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun stateInitialized() {
        isPlaylistReady = true
    }

    // Станция, которую экран последний раз попросил включить. Плейер узнаёт о ней не сразу: команды MediaBrowser асинхронные,
    // а если плейлист ещё скачивается, сессия ждёт его. Нужна MainActivity, чтобы новый плейлист открылся именно на этой станции
    var requestedStation: RadioStationPresentation? = null
        private set

    // Включить станцию, поставить на паузу или продолжить. Вызывается в главном потоке
    fun playOrToggleSong(mediaItem: RadioStationPresentation, toggle: Boolean = false) {
        val playbackState = playbackState.value
        val isPrepared = playbackState?.isPrepared ?: false

        // if we want to play the same song (pause and play it again)
        if (playbackState != null && isPrepared && mediaItem.stationuuid == curPlayingSong.value?.mediaId) {
            Log.d(TAG, "Включаем/выключаем ту же самую станцию ${mediaItem.stationName}")

            when {
                playbackState.isPlaying -> {
                    // Станция одна и та же, но одна из них из избранного, а другая нет (разные плейлисты) - включаем её заново,
                    // иначе в уведомлении и в плейере окажутся разные плейлисты
                    val isCurCountryCodeFAV =
                        curPlayingSong.value?.mediaMetadata?.subtitle.toString()
                            .endsWith("_FAV", true)
                    val isToggleCountryCodeFAV = mediaItem.countryCode.endsWith("_FAV", true)
                    if (isCurCountryCodeFAV != isToggleCountryCodeFAV) {
                        Log.d(
                            TAG,
                            "Станция одна и та же, но одна из них не из избранного: ${mediaItem.stationName}, ${mediaItem.countryCode}"
                        )
                        requestedStation = mediaItem
                        musicServiceConnection.playFromMediaId(
                            mediaItem.stationuuid,
                            mediaItem.stationName,
                            mediaItem.countryCode
                        )
                    }

                    if (toggle) musicServiceConnection.pause()
                }

                playbackState.isPlayEnabled -> musicServiceConnection.play()
            }

            saveLastUsedRadioStationUrlAndCode(mediaItem.urlResolved, mediaItem.countryCode)
            _switchViewPagerOnceAgain.trySend(mediaItem)
            hideProgressAndSetClickable()
        } else {
            // if we want to play another song
            Log.d(TAG, "Включаем другую станцию ${mediaItem.stationName}")
            requestedStation = mediaItem
            musicServiceConnection.playFromMediaId(
                mediaItem.stationuuid,
                mediaItem.stationName,
                mediaItem.countryCode
            )
            saveLastUsedRadioStationUrlAndCode(mediaItem.urlResolved, mediaItem.countryCode)
            _switchViewPagerOnceAgain.trySend(mediaItem)
        }
    }

    private fun saveLastUsedRadioStationUrlAndCode(urlResolved: String, countryCode: String) {
        viewModelScope.launch {
            mainRadioInteractor.saveLastUsedRadioStationUrlAndCode(urlResolved, countryCode)
        }
    }

    // To change a playlist
    fun fetchSongs(countryCode: String) {
        val args = Bundle().apply {
            putString(COUNTRY_CODE_ID, countryCode)
        }
        musicServiceConnection.sendCommand(ADD_SONGS, args)
    }

    // Полоса "Downloading playlist"
    fun showDownloadingPlaylistProgress() = showProgress(LoadingState.DOWNLOADING_PLAYLIST)

    // Полоса "Connecting to radio station". Её нужно спрятать по первому же ответу плейера (см. MainActivity)
    fun showConnectingProgress() = showProgress(LoadingState.CONNECTING_STATION)

    private fun showProgress(state: LoadingState) {
        connectingProgressShownAt =
            if (state == LoadingState.CONNECTING_STATION) SystemClock.elapsedRealtime() else null
        _loadingState.value = state

        // Страховка: полоса загрузки перекрывает весь экран, и если по какой-то причине её не убрали,
        // приложением невозможно пользоваться. Через PROGRESS_TIMEOUT прячем её сами и показываем ошибку
        progressTimeoutJob?.cancel()
        progressTimeoutJob = viewModelScope.launch {
            delay(PROGRESS_TIMEOUT)
            Log.d(
                TAG,
                "Прогресс висит дольше $PROGRESS_TIMEOUT мс - прячем его и показываем ошибку"
            )
            _errorMessages.trySend("Loading is taking too long. Please check your internet connection and try again")
            if (state == LoadingState.DOWNLOADING_PLAYLIST) {
                // Отменяем загрузку в сервисе, чтобы её результат (например, диалог "получен пустой список")
                // не появился позже, когда пользователь уже делает что-то другое
                musicServiceConnection.sendCommand(CANCEL_PLAYLIST_DOWNLOAD, null)
            }
            hideProgressAndSetClickable()
        }
    }

    // Плейлист показан в плейере. Полосу "Downloading playlist" убираем, но если станция ещё подключается (буферизация),
    // сменяем её на "Connecting to radio station" - её уберёт первый звук или ошибка плейера (см. MainActivity).
    // Раньше полоса пропадала сразу, и плейер выглядел играющим, хотя звука ещё не было.
    // "Connecting to radio station" здесь не трогаем - только плейер знает, когда станция заиграла
    fun onPlaylistShown() {
        if (_loadingState.value != LoadingState.DOWNLOADING_PLAYLIST) return
        val state = playbackState.value
        if (state != null && state.isPlaying && !state.isActuallyPlaying && !state.hasError) {
            showConnectingProgress()
        } else {
            hideProgressAndSetClickable()
        }
    }

    fun hideProgressAndSetClickable(isServerDown: Boolean = false) {
        progressTimeoutJob?.cancel()
        connectingProgressShownAt = null
        _loadingState.value = LoadingState.NONE
        this.isServerDown = isServerDown
    }

    fun notJustLaunchedEnableAutoplay() {
        _isNotJustLaunched.value = true
    }

    fun checkIsStationInFavouritesAndChangeTheStar(currentRadioStation: RadioStationPresentation) {
        viewModelScope.launch {
            // Если станция есть в избранном и нажали на звезду, нужно из избранного удалить и убрать звезду, и наоборот
            val isFavourite = !currentRadioStation.isStationInFavourite
            changeFavouriteInteractor.setFavourite(currentRadioStation.toDomain(), isFavourite)
            notifyFavouriteChanged(currentRadioStation, isFavourite)
        }
    }

    // Сообщить всем экранам, что станция добавлена в избранное или убрана из него (изменение в базе уже сделано)
    fun notifyFavouriteChanged(station: RadioStationPresentation, isFavourite: Boolean) {
        // В плейлисте избранного код страны с суффиксом "_FAV" - в самом событии он не нужен (по нему список избранного показывает страну)
        val changedStation = station.copy(
            isStationInFavourite = isFavourite,
            countryCode = station.countryCode.removeSuffix("_FAV")
        )
        _favouriteChanges.tryEmit(FavouriteChange(changedStation, isFavourite))

        // Звезда в плейлисте плеера: создаём новый список с изменённой станцией. Раньше MainActivity меняла поле
        // у объекта станции прямо в списке адаптера
        _playlist.value?.let { playlist ->
            _playlist.value = playlist.copy(
                stations = playlist.stations.map {
                    if (it.stationuuid == station.stationuuid) it.copy(isStationInFavourite = isFavourite) else it
                }
            )
        }
    }

    // Отметка "станция популярна" на сервере radio-browser (по просьбе автора API). На работу приложения не влияет
    fun markRadioStationAsPopularSendGetRequest(stationUuid: String) {
        viewModelScope.launch {
            isServerDown = mainRadioInteractor.markRadioStationAsPopularSendGetRequest(stationUuid)
        }
    }

    // when View model is destroyed - заканчиваем нашу связь с сервисом
    override fun onCleared() {
        musicServiceConnection.unsubscribe(MEDIA_ROOT_ID, onChildrenLoaded)
        super.onCleared()
    }
}