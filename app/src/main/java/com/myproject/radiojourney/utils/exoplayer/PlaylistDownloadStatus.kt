package com.myproject.radiojourney.utils.exoplayer

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

    // Событие "не удалось скачать плейлист: сервер недоступен"
    private val _serverIsDown = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val serverIsDown: SharedFlow<Unit> = _serverIsDown.asSharedFlow()

    fun resetProgress() {
        _progressPercent.value = 0
    }

    fun setProgress(processedCount: Int, listSize: Int) {
        if (listSize > 0) _progressPercent.value = 100 * processedCount / listSize
    }

    fun notifyServerIsDown() {
        _serverIsDown.tryEmit(Unit)
    }
}
