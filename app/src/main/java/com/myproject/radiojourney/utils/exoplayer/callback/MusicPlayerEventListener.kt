package com.myproject.radiojourney.utils.exoplayer.callback

import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource.HttpDataSourceException
import androidx.media3.exoplayer.source.UnrecognizedInputFormatException
import com.myproject.radiojourney.utils.exoplayer.MusicService

/**
 * Слушатель ошибок плеера: станция не отвечает, поток не открывается, нет интернета.
 *
 * Показывает пользователю toast, а сервису сообщает, что воспроизведение не началось
 */
class MusicPlayerEventListener(
    private val musicService: MusicService
) : Player.Listener {

    companion object {
        private const val TAG = "MusicPlayerEventLis-r"
    }

    // !!!!!!!!!!!!!! Раньше здесь при паузе вызывался stopForeground(). Сервис переставал быть foreground, и Xiaomi (MIUI) при смахивании
    // приложения сразу убивал процесс: уведомление оставалось висеть "мёртвым" - не смахивалось, кнопки не работали.
    // Теперь на паузе сервис остаётся foreground, а уведомление убирается само через PAUSED_NOTIFICATION_TIMEOUT (MusicService)

    // TODO onPlayerError - обработать /  Обрабатывала в другом месте - проверить, нужно ли
    @OptIn(UnstableApi::class) // UnrecognizedInputFormatException
    override fun onPlayerError(error: PlaybackException) {
        when (error.cause) {
            is UnrecognizedInputFormatException -> {
                Log.d(
                    TAG,
                    "An error occurred in onPlayerError. UnrecognizedInputFormatException is caught. PrintStackTrace:"
                )
                Toast.makeText(musicService, "Exoplayer can't read the stream", Toast.LENGTH_LONG)
                    .show()
            }

            is HttpDataSourceException -> {
                Log.d(
                    TAG,
                    "An error occurred in onPlayerError. HttpDataSourceException is caught. PrintStackTrace:"
                )
                Toast.makeText(musicService, "Exoplayer can't read this url", Toast.LENGTH_LONG)
                    .show()
            }

            else -> {
                Log.d(
                    TAG,
                    "An unknown error occurred in onPlayerError. Not UnrecognizedInputFormatException is caught. PrintStackTrace:"
                )
                Toast.makeText(musicService, "An unknown error occurred", Toast.LENGTH_LONG).show()
            }
        }
        error.printStackTrace()
    }

}