package com.myproject.radiojourney.utils.exoplayer.callback

import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.os.ResultReceiver
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.myproject.radiojourney.utils.exoplayer.FirebaseMusicSource

class MusicPlaybackPreparer(
    private val firebaseMusicSource: FirebaseMusicSource,
    private val playerPrepared: (MediaMetadataCompat?) -> Unit // lambda, that can be called when our player is prepared
) : MediaSessionConnector.PlaybackPreparer {

    // we won't implement now
    override fun onCommand(
        player: Player,
        controlDispatcher: ControlDispatcher,
        command: String,
        extras: Bundle?,
        cb: android.os.ResultReceiver?
    ): Boolean = false

    // return a tab of actions that we support in our player
    override fun getSupportedPrepareActions(): Long {
        // we prepare a specific song with media id (that's why we added media id earlier) or play it
        // so, media id is used to select a specific song
        return PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
    }

    // we won't implement now
    override fun onPrepare(playWhenReady: Boolean) = Unit

    // function for preparing song that user selected
    override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle?) {
        // here we will need our function with stated from enum class (STATE_CREATED, STATE_INITIALIZING, STATE_INITIALIZED, STATE_ERROR)
        firebaseMusicSource.whenReady {
            // looking for a song with media id
            val itemToPlay = firebaseMusicSource.radioStations.find { mediaId == it.description.mediaId }
            playerPrepared(itemToPlay)
        }
    }

    // we won't implement now (from google voice, for instance)
    override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) = Unit

    // we won't implement now
    override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) = Unit
}
