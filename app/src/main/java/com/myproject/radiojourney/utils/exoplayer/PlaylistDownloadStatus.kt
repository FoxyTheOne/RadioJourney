package com.myproject.radiojourney.utils.exoplayer

import com.myproject.radiojourney.domain.model.RadioStationList
import com.myproject.radiojourney.other.ServerError
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Состояние загрузки плейлиста: от сервиса плеера (FirebaseMusicSource) к экрану (MainViewModel -> MainActivity).
 *
 * Раньше сервис отправлял это бродкастами (обычным и LocalBroadcastManager). Сервис и экран работают в одном процессе,
 * поэтому достаточно общего объекта (@Singleton) с Flow: не нужны Intent, ключи и регистрация приёмников,
 * а LocalBroadcastManager устарел (developer.android.com рекомендует заменить его на наблюдаемые данные, например Flow)
 */
@Singleton
class PlaylistDownloadStatus @Inject constructor() {

    // Сколько процентов станций плейлиста обработано: 0..100
    private val _progressPercent = MutableStateFlow(0)
    val progressPercent: StateFlow<Int> = _progressPercent.asStateFlow()

    // Событие "не удалось скачать плейлист" с причиной: нет сети, сервер не отвечает или ответ обрывается
    private val _serverIsDown = MutableSharedFlow<ServerError>(extraBufferCapacity = 1)
    val serverIsDown: SharedFlow<ServerError> = _serverIsDown.asSharedFlow()

    // Событие "плейлист не свежий и полный, а запасной": сохранённый при прошлом скачивании или только самые популярные станции
    // (см. RadioStationList.isFallback). Экран скажет об этом пользователю
    private val _fallbackPlaylistUsed = MutableSharedFlow<RadioStationList>(extraBufferCapacity = 1)
    val fallbackPlaylistUsed: SharedFlow<RadioStationList> = _fallbackPlaylistUsed.asSharedFlow()

    fun resetProgress() {
        _progressPercent.value = 0
    }

    fun setProgress(processedCount: Int, listSize: Int) {
        if (listSize > 0) _progressPercent.value = 100 * processedCount / listSize
    }

    fun notifyServerIsDown(reason: ServerError) {
        _serverIsDown.tryEmit(reason)
    }

    fun notifyFallbackPlaylistUsed(radioStationList: RadioStationList) {
        _fallbackPlaylistUsed.tryEmit(radioStationList)
    }
}
