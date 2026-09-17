package com.myproject.radiojourney.utils.exoplayer

import androidx.media3.common.Player

/**
 * Состояние плеера для экрана (MusicServiceConnection.playbackStateLiveData).
 * Раньше экран получал PlaybackStateCompat, и для него были расширения isPrepared / isPlaying / isPlayEnabled
 * (PlaybackStateCompatExtension.kt). С media3 состояние берётся из MediaBrowser (Player) и складывается сюда,
 * свойства с теми же названиями и тем же смыслом.
 */
data class PlaybackStateInfo(
    val playbackState: Int, // Player.STATE_IDLE / STATE_BUFFERING / STATE_READY / STATE_ENDED
    val playWhenReady: Boolean,
    val isActuallyPlaying: Boolean, // Player.isPlaying: звук действительно идёт
    val hasError: Boolean, // плеер остановился с ошибкой (Player.playerError != null)
    val updateTime: Long // SystemClock.elapsedRealtime(), когда состояние получено
) {
    // is our music in onPrepared state? (раньше PlaybackStateCompat STATE_BUFFERING / STATE_PLAYING / STATE_PAUSED)
    val isPrepared: Boolean
        get() = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_READY

    // играет или загружается, чтобы играть (раньше PlaybackStateCompat STATE_BUFFERING / STATE_PLAYING)
    val isPlaying: Boolean
        get() = playWhenReady && isPrepared

    // if the option of playing songs is enabled - плеер не играет, можно нажать play
    val isPlayEnabled: Boolean
        get() = !isPlaying
}