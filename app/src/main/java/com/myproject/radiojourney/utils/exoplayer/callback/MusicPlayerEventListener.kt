package com.myproject.radiojourney.utils.exoplayer.callback

import android.app.Service
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException
import com.myproject.radiojourney.utils.exoplayer.MusicService

class MusicPlayerEventListener(
    private val musicService: MusicService
) : Player.Listener {

    companion object {
        private const val TAG = "MusicPlayerEventLis-r"
    }

    private var playWhenReadySaved = true

    // default void onPlayerStateChanged(boolean playWhenReady, @State int playbackState) {} is DEPRECATED.
    // Use onPlaybackStateChanged(int) and onPlayWhenReadyChanged(boolean, int) instead:
    // - default void onPlaybackStateChanged(@State int playbackState) {}
    // - default void onPlayWhenReadyChanged(
    //        boolean playWhenReady, @PlayWhenReadyChangeReason int reason) {}
    //
//    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
//        super.onPlayerStateChanged(playWhenReady, playbackState)
//        // if everything is ready and prepared AND we shouldn't play it automatically
//        if (playbackState == Player.STATE_READY && !playWhenReady) {
//            musicService.stopForeground(false) // than we stop foreground but notification must stay
//        }
//    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        super.onPlayWhenReadyChanged(playWhenReady, reason)

        playWhenReadySaved = playWhenReady
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)

        // if everything is ready and prepared AND we shouldn't play it automatically
        if (playbackState == Player.STATE_READY && !playWhenReadySaved) {

            musicService.apply {
                // stopForeground(false) - deprecated
                // STOP_FOREGROUND_DETACH if set, the notification previously supplied to startForeground(int, Notification) will be detached from the service's lifecycle.
                // The notification will remain shown even after the service is stopped and destroyed.
                // STOP_FOREGROUND_REMOVE if supplied, the notification previously supplied to startForeground(int, Notification) will be cancelled and removed from display.
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(Service.STOP_FOREGROUND_DETACH) // than we stop foreground but notification must stay
                } else {
                    stopForeground(false) // than we stop foreground but notification must stay
                }
            }

        }

    }

    // TODO onPlayerError - обработать
    override fun onPlayerError(error: PlaybackException) {
        if (error.cause is UnrecognizedInputFormatException) {
            Log.d(
                TAG,
                "UnrecognizedInputFormatException is caught. PrintStackTrace:"
            )
            Toast.makeText(musicService, "Exoplayer can't read the stream", Toast.LENGTH_LONG)
                .show()
            musicService.testMethodForError()
        } else {
            Log.d(
                TAG,
                "Not UnrecognizedInputFormatException is caught. PrintStackTrace:"
            )
            Toast.makeText(musicService, "An unknown error occurred", Toast.LENGTH_LONG).show()
        }
        error.printStackTrace()
//        when (error.type) {
//            ExoPlaybackException.TYPE_SOURCE -> Log.e(TAG, "TYPE_SOURCE: " + error.sourceException.message)
//
//            ExoPlaybackException.TYPE_RENDERER -> Log.e(TAG, "TYPE_RENDERER: " + error.rendererException.message)
//
//            ExoPlaybackException.TYPE_UNEXPECTED -> Log.e(TAG, "TYPE_UNEXPECTED: " + error.unexpectedException.message)
//            ExoPlaybackException.TYPE_OUT_OF_MEMORY -> {
//                TODO()
//            }
//            ExoPlaybackException.TYPE_REMOTE -> {
//                TODO()
//            }
//        }

//        if (error.getCause() is BehindLiveWindowException) {
//            Toast.makeText(mContext, R.string.stream_failure_retry, Toast.LENGTH_SHORT).show()
//            mProviderTvPlayer.restart(mContext)
//            mProviderTvPlayer.play()
//        } else if (error.getCause() is UnrecognizedInputFormatException) {
//            // Channel cannot be played in case of an error in parsing the ".m3u8" file.
//            Toast.makeText(
//                mContext,
//                mContext.getString(R.string.channel_stream_failure),
//                Toast.LENGTH_SHORT
//            ).show()
//        } else if (error.getCause() is InvalidResponseCodeException) {
//
//            /*
//          We might get errors different like 403 which indicate permission denied due to multiple
//          connections at the same time or 502 meaning bad gateway.
//
//          Restart the loading after 5 seconds.
//          */
//            val handler = Handler()
//            handler.postDelayed(Runnable {
//                if (mContext != null && mProviderTvPlayer != null) {
//                    Toast.makeText(mContext, R.string.stream_failure_retry, Toast.LENGTH_SHORT)
//                        .show()
//                    mProviderTvPlayer.restart(mContext)
//                    mProviderTvPlayer.play()
//                }
//            }, 5000)
//        } else if (error.getCause() is HttpDataSourceException) {
//            // Timeout, nothing we can do really...
//            Toast.makeText(mContext, R.string.channel_stream_failure, Toast.LENGTH_SHORT).show()
//        }
    }

}