package com.myproject.radiojourney.utils.exoplayer.callback

import android.widget.Toast
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.myproject.radiojourney.utils.exoplayer.MusicService

class MusicPlayerEventListener(
    private val musicService: MusicService
) : Player.EventListener {

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        super.onPlayerStateChanged(playWhenReady, playbackState)
        // if everything is ready and prepared AND we shouldn't play it automatically
        if (playbackState == Player.STATE_READY && !playWhenReady) {
            musicService.stopForeground(false) // than we stop foreground but notification must stay
        }
    }

    // TODO onPlayerError - обработать
    override fun onPlayerError(error: ExoPlaybackException) {
        super.onPlayerError(error)
        Toast.makeText(musicService, "An unknown error occurred", Toast.LENGTH_LONG).show()
    }
}