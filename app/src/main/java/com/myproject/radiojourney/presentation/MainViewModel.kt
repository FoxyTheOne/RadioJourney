package com.myproject.radiojourney.presentation

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.google.android.gms.maps.model.CameraPosition
import com.myproject.radiojourney.data.worker.CountryCacheScheduler
import com.myproject.radiojourney.domain.changeFavouriteUseCase.IChangeFavouriteUseCase
import com.myproject.radiojourney.domain.firstScreenLoadingUseCase.ILoginScreenUseCase
import com.myproject.radiojourney.domain.mainRadioUseCase.IMainRadioUseCase
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import com.myproject.radiojourney.other.Constants.ADD_SONGS
import com.myproject.radiojourney.other.Constants.CANCEL_PLAYLIST_DOWNLOAD
import com.myproject.radiojourney.other.Constants.COUNTRY_CODE_ID
import com.myproject.radiojourney.other.Constants.MEDIA_ROOT_ID
import com.myproject.radiojourney.other.Constants.PROGRESS_TIMEOUT
import com.myproject.radiojourney.other.Event
import com.myproject.radiojourney.other.Resource
import com.myproject.radiojourney.utils.exoplayer.MusicServiceConnection
import com.myproject.radiojourney.utils.exoplayer.PlaylistDownloadStatus
import com.myproject.radiojourney.utils.extension.call
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Основная ViewModel (привязана к MainActivity): плейер в нижней панели, плейлист, полосы загрузки, избранное.
 * Удалены неиспользуемые LiveData (messageLiveData, dataSavedSuccessfulLiveData, newMediaIdLiveData и др.),
 * блоки catch (AccountsException / IOException), которые не могли сработать, и закомментированный старый код
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

    // Прогресс загрузки плейлиста (0..100) и ошибка "сервер недоступен" - из сервиса плеера. Раньше приходили бродкастами в MainActivity
    val playlistDownloadProgressLiveData: LiveData<Int> =
        playlistDownloadStatus.progressPercent.asLiveData()

    private val _serverIsDownLiveData = MutableLiveData<Event<Boolean>>()
    val serverIsDownLiveData: LiveData<Event<Boolean>> = _serverIsDownLiveData

    // Станции текущего плейлиста для ViewPager в MainActivity и списков
    private val _mediaItemsListLiveData =
        MutableLiveData<Resource<List<RadioStationPresentation>>>()
    val mediaItemsListLiveData: LiveData<Resource<List<RadioStationPresentation>>> =
        _mediaItemsListLiveData

    // Нужно вызывать playOrToggleSong, когда у нас новый плейлист, а станция была на паузе. И в то же время не нужно autoplay сразу при запуске программы
    private val _isNotJustLaunchedLiveData = MutableLiveData<Boolean>()
    val isNotJustLaunchedLiveData: LiveData<Boolean> = _isNotJustLaunchedLiveData

    // Одно общее событие "станция добавлена в избранное / убрана из избранного" - где бы ни нажали звезду
    // (плейер, список избранного). В нём есть КАКАЯ станция изменилась: раньше плейер получал только
    // "звезда да/нет" и менял звезду у своей станции, даже если в списке избранного нажали на другую.
    // id растёт с каждым изменением: подписчик, появившийся позже (например, заново открытый список избранного),
    // пропускает изменения, которые были до него. LiveData повторяет новому подписчику последнее значение,
    // и раньше при открытии списка избранного старое событие заново ставило/убирало звезду у играющей станции
    data class FavouriteChange(
        val id: Long,
        val station: RadioStationPresentation,
        val isFavourite: Boolean
    )

    private var lastFavouriteChangeId = 0L
    private val _favouriteChangeLiveData = MutableLiveData<FavouriteChange>()
    val favouriteChangeLiveData: LiveData<FavouriteChange> = _favouriteChangeLiveData

    // id последнего уже случившегося изменения - подписчик пропускает события с id не больше этого
    val lastFavouriteChangeIdForNewObserver: Long
        get() = _favouriteChangeLiveData.value?.id ?: 0L

    // Иногда сбивается и в уведомлении показывает правильную станцию, а в плейере - нет. Страховка: MainActivity ещё раз переключает ViewPager
    private val _switchViewPagerOnceAgainLiveData = MutableLiveData<RadioStationPresentation>()
    val switchViewPagerOnceAgainLiveData: LiveData<RadioStationPresentation> =
        _switchViewPagerOnceAgainLiveData

    // LiveData from our ServiceConnection
    val isConnectedLiveData = musicServiceConnection.isConnectedLiveData
    val networkErrorLiveData = musicServiceConnection.networkErrorLiveData
    val playbackStateLiveData = musicServiceConnection.playbackStateLiveData
    val curPlayingSongLiveData = musicServiceConnection.curPlayingSongLiveData

    // If smth went wrong
    private val _errorMessageLiveData = MutableLiveData<Event<Resource<Boolean>>>()
    val errorMessageLiveData: LiveData<Event<Resource<Boolean>>> = _errorMessageLiveData

    var isServerDown = false
        private set

    private var progressTimeoutJob: Job? = null

    // Время (SystemClock.elapsedRealtime), когда показали полосу "Connecting to radio station". null - сейчас не показана
    var connectingProgressShownAt: Long? = null
        private set

    // Позиция карты на главном экране. HomeRadioFragment создаётся заново при каждом возвращении на него,
    // поэтому храним позицию здесь (MainViewModel живёт, пока открыта MainActivity)
    var mapCameraPosition: CameraPosition? = null

    // Полосы загрузки: "Downloading playlist" (Dp) и "Connecting to radio station" (CRSt), и их скрытие
    private val _setClickableLiveData = MutableLiveData<Boolean>()
    val setClickableLiveData: LiveData<Boolean> = _setClickableLiveData
    private val _setNonClickableDpLiveData = MutableLiveData<Boolean>()
    val setNonClickableDpLiveData: LiveData<Boolean> = _setNonClickableDpLiveData
    private val _setNonClickableCRStLiveData = MutableLiveData<Boolean>()
    val setNonClickableCRStLiveData: LiveData<Boolean> = _setNonClickableCRStLiveData

    // Плейлист хотя бы раз показан на экране (в ViewPager).
    // Раньше здесь был список лямбд whenReady: ViewModel хранила лямбды из MainActivity (а значит, и саму Activity) и не очищала список.
    // ViewModel живёт дольше Activity (пересоздание при смене темы, языка), поэтому старая Activity оставалась в памяти,
    // а лямбды вызывались повторно при каждом новом плейлисте. Теперь ViewModel хранит только флаг, а ожидающие действия - в MainActivity
    var isPlaylistReady = false
        private set

    // Получатель списка станций от сервиса (MusicServiceConnection.subscribe). Отдельное поле - чтобы в onCleared()
    // отписать именно его: MusicServiceConnection один на всё приложение, а MainViewModel создаётся заново вместе с MainActivity
    private val onChildrenLoaded: (List<MediaItem>) -> Unit = { children ->
        viewModelScope.launch {
            // Данные подтягиваются из MusicLibrarySessionCallback.onGetChildren() в MusicService.
            // Преобразуем MediaItem (media3) в наш формат данных
            val radioStationPresentationList =
                mainRadioInteractor.mediaItemChildrenToRadioStationPresentation(children)
            _mediaItemsListLiveData.value = Resource.success(radioStationPresentationList)
            Log.d(
                TAG,
                "PLAYLIST_UPDATE: 3.$TAG, onChildrenLoaded(). Данные загружены, кладём их в mediaItemsListLiveData"
            )
        }
    }

    init {
        viewModelScope.launch {
            playlistDownloadStatus.serverIsDown.collect {
                _serverIsDownLiveData.value = Event(true)
            }
        }

        // Загрузка списка стран для карты. MainViewModel создаётся один раз за запуск приложения (переживает пересоздание Activity),
        // поэтому загрузка не повторяется при смене темы или языка, как было с запуском сервиса в MainActivity.onCreate
        countryCacheScheduler.start()

        _mediaItemsListLiveData.value = Resource.loading(null)
        // Список радиостанций после загрузки плейлиста - для обновления UI (например, ViewPager)
        musicServiceConnection.subscribe(MEDIA_ROOT_ID, onChildrenLoaded)
    }

    // Пользователь уже входил - первый экран (загрузка и вход) пропускаем
    fun isLoggedIn(): Boolean = loginScreenInteractor.isLoggedIn()

    fun stateInitialized() {
        isPlaylistReady = true
    }

    // Включить станцию, поставить на паузу или продолжить. Вызывается в главном потоке.
    // Раньше метод запускался в Dispatchers.IO и читал LiveData.value из фонового потока внутри synchronized(mediaItem),
    // хотя команды плееру всё равно выполняются в главном потоке (MusicServiceConnection)
    fun playOrToggleSong(mediaItem: RadioStationPresentation, toggle: Boolean = false) {
        val playbackState = playbackStateLiveData.value
        val isPrepared = playbackState?.isPrepared ?: false

        // if we want to play the same song (pause and play it again)
        if (playbackState != null && isPrepared && mediaItem.stationuuid == curPlayingSongLiveData.value?.mediaId) {
            Log.d(TAG, "Включаем/выключаем ту же самую станцию ${mediaItem.stationName}")

            when {
                playbackState.isPlaying -> {
                    // Станция одна и та же, но одна из них из избранного, а другая нет (разные плейлисты) - включаем её заново,
                    // иначе в уведомлении и в плейере окажутся разные плейлисты
                    val isCurCountryCodeFAV =
                        curPlayingSongLiveData.value?.mediaMetadata?.subtitle.toString()
                            .endsWith("_FAV", true)
                    val isToggleCountryCodeFAV = mediaItem.countryCode.endsWith("_FAV", true)
                    if (isCurCountryCodeFAV != isToggleCountryCodeFAV) {
                        Log.d(
                            TAG,
                            "Станция одна и та же, но одна из них не из избранного: ${mediaItem.stationName}, ${mediaItem.countryCode}"
                        )
                        musicServiceConnection.playFromMediaId(mediaItem.stationuuid)
                        if (toggle) musicServiceConnection.pause()
                        _switchViewPagerOnceAgainLiveData.postValue(mediaItem)
                    }

                    if (toggle) musicServiceConnection.pause()
                }

                playbackState.isPlayEnabled -> musicServiceConnection.play()
            }

            saveLastUsedRadioStationUrlAndCode(mediaItem.urlResolved, mediaItem.countryCode)
            _switchViewPagerOnceAgainLiveData.postValue(mediaItem)
            hideProgressAndSetClickable()
        } else {
            // if we want to play another song
            Log.d(TAG, "Включаем другую станцию ${mediaItem.stationName}")
            musicServiceConnection.playFromMediaId(mediaItem.stationuuid)
            saveLastUsedRadioStationUrlAndCode(mediaItem.urlResolved, mediaItem.countryCode)
            _switchViewPagerOnceAgainLiveData.postValue(mediaItem)
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

    fun showProgressAndDisableClick(stringDpOrCRSt: String) {
        // Запоминаем, когда показали "Connecting to radio station": её нужно спрятать по первому же ответу плейера (см. MainActivity)
        connectingProgressShownAt =
            if (stringDpOrCRSt.lowercase() == "crst") SystemClock.elapsedRealtime() else null

        when (stringDpOrCRSt.lowercase()) {
            "dp" -> _setNonClickableDpLiveData.call()
            "crst" -> _setNonClickableCRStLiveData.call()
            else -> Log.d(TAG, "Unknown String in showProgressAndDisableClick()")
        }

        // Страховка: полоса загрузки перекрывает весь экран, и если по какой-то причине её не убрали,
        // приложением невозможно пользоваться. Через PROGRESS_TIMEOUT прячем её сами и показываем ошибку
        progressTimeoutJob?.cancel()
        progressTimeoutJob = viewModelScope.launch {
            delay(PROGRESS_TIMEOUT)
            Log.d(
                TAG,
                "Прогресс висит дольше $PROGRESS_TIMEOUT мс - прячем его и показываем ошибку"
            )
            errorMessagePost("Loading is taking too long. Please check your internet connection and try again")
            if (connectingProgressShownAt == null) {
                // Это была полоса "Downloading playlist": отменяем загрузку в сервисе, чтобы её результат
                // (например, диалог "получен пустой список") не появился позже, когда пользователь уже делает что-то другое
                musicServiceConnection.sendCommand(CANCEL_PLAYLIST_DOWNLOAD, null)
            }
            hideProgressAndSetClickable()
        }
    }

    fun hideProgressAndSetClickable(isServerDown: Boolean = false) {
        progressTimeoutJob?.cancel()
        connectingProgressShownAt = null
        _setClickableLiveData.call()
        this.isServerDown = isServerDown
    }

    fun notJustLaunchedEnableAutoplay() {
        _isNotJustLaunchedLiveData.postValue(true)
    }

    fun checkIsStationInFavouritesAndChangeTheStar(currentRadioStation: RadioStationPresentation) {
        viewModelScope.launch {
            // Если станция есть в избранном и нажали на звезду, нужно из избранного удалить и убрать звезду, и наоборот
            val isFavourite = !currentRadioStation.isStationInFavourite
            changeFavouriteInteractor.setFavourite(currentRadioStation, isFavourite)
            notifyFavouriteChanged(currentRadioStation, isFavourite)
        }
    }

    // Сообщить всем экранам, что станция добавлена в избранное или убрана из него (изменение в базе уже сделано)
    fun notifyFavouriteChanged(station: RadioStationPresentation, isFavourite: Boolean) {
        // setValue в главном потоке, а не postValue: postValue из двух быстрых изменений доставляет только последнее
        lastFavouriteChangeId++
        _favouriteChangeLiveData.value = FavouriteChange(
            lastFavouriteChangeId,
            // В плейлисте избранного код страны с суффиксом "_FAV" - в самом событии он не нужен (по нему список избранного показывает страну)
            station.copy(
                isStationInFavourite = isFavourite,
                countryCode = station.countryCode.removeSuffix("_FAV")
            ),
            isFavourite
        )
    }

    fun errorMessagePost(message: String) {
        _errorMessageLiveData.postValue(Event(Resource.error(message, null)))
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