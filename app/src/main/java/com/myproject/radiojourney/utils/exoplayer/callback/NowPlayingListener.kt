package com.myproject.radiojourney.utils.exoplayer.callback

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.myproject.radiojourney.utils.exoplayer.cleanStreamTitle
import com.myproject.radiojourney.utils.exoplayer.nowPlayingTitle
import com.myproject.radiojourney.utils.exoplayer.streamTitle
import com.myproject.radiojourney.utils.exoplayer.withNowPlaying

/**
 * Следит за названием песни, которое станция присылает в потоке (см. NowPlaying.kt), и записывает его в текущую станцию плеера.
 *
 * Почему название записывается в саму станцию (MediaItem), а не передаётся отдельно: уведомление, экран блокировки,
 * часы и экран приложения и так получают текущую станцию от медиасессии. Обновили станцию - обновились все.
 *
 * replaceMediaItem с тем же адресом потока ExoPlayer выполняет "на ходу": меняет только данные станции,
 * а звук не прерывается и не буферизуется заново.
 *
 * Важное правило: показываем только то, что станция сообщает прямо сейчас. Прислала пустое
 * название (реклама, джингл) - название убирается, а не остаётся висеть от прошлой песни. Иначе пользователь видел бы
 * старую песню, когда играет уже другая
 */
class NowPlayingListener(private val player: Player) : Player.Listener {

    companion object {
        private const val TAG = "NowPlayingListener"
    }

    private var lastRawTitle: String? = null

    // Метаданные потока приходят несколько раз в минуту, часто с тем же названием - обновляем станцию, только когда оно изменилось.
    // @OptIn: класс Metadata в media3 помечен @UnstableApi (его могут изменить в новой версии), см. ServiceModule
    @OptIn(UnstableApi::class)
    override fun onMetadata(metadata: Metadata) {
        val index = player.currentMediaItemIndex
        val item = player.currentMediaItem ?: return
        val stationName = item.mediaMetadata.title?.toString().orEmpty()
        val rawTitle = metadata.streamTitle()
        // В лог - то, что станция прислала на самом деле (только когда это изменилось): по нему видно, почему названия нет
        if (rawTitle != lastRawTitle) {
            lastRawTitle = rawTitle
            Log.d(TAG, "Станция $stationName прислала: $rawTitle")
        }
        val title = cleanStreamTitle(rawTitle, stationName)
        if (title == item.mediaMetadata.nowPlayingTitle) return

        Log.d(TAG, "Станция $stationName, сейчас играет: $title")
        player.replaceMediaItem(index, item.withNowPlaying(title))
    }

    /**
     * Переключились на другую станцию внутри плейлиста: кнопками "вперёд/назад" в уведомлении, на наушниках, на часах.
     * Название песни хранится в самой станции плейлиста, поэтому у станции, которую оставили, оно бы так и осталось.
     * Вернулись бы к ней - и сначала увидели бы песню, которая играла там когда-то. Стираем названия у всех станций,
     * кроме текущей: у текущей название свежее, его только что прислала станция (или его пока нет).
     *
     * Только для переключений (SEEK) и автоперехода (AUTO). PLAYLIST_CHANGED сюда не относится: так ExoPlayer сообщает
     * и о replaceMediaItem из onMetadata - стирали бы только что записанное название
     */
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_SEEK && reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) return
        lastRawTitle = null
        for (index in 0 until player.mediaItemCount) {
            if (index == player.currentMediaItemIndex) continue
            val item = player.getMediaItemAt(index)
            if (item.mediaMetadata.nowPlayingTitle != null) player.replaceMediaItem(
                index,
                item.withNowPlaying(null)
            )
        }
    }
}