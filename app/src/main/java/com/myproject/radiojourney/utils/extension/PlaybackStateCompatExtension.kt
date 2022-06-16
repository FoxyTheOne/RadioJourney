package com.myproject.radiojourney.utils.extension

import android.os.SystemClock
import android.support.v4.media.session.PlaybackStateCompat

// is our music in onPrepared state?
inline val PlaybackStateCompat.isPrepared
    get() = state == PlaybackStateCompat.STATE_BUFFERING ||
            state == PlaybackStateCompat.STATE_PLAYING ||
            state == PlaybackStateCompat.STATE_PAUSED // inline val => get() will be inline here

inline val PlaybackStateCompat.isPlaying
    get() = state == PlaybackStateCompat.STATE_BUFFERING ||
            state == PlaybackStateCompat.STATE_PLAYING

inline val PlaybackStateCompat.isPlayEnabled // if the option of playing songs is enabled
    get() = actions and PlaybackStateCompat.ACTION_PLAY != 0L ||
            (actions and PlaybackStateCompat.ACTION_PLAY_PAUSE != 0L &&
                    state == PlaybackStateCompat.STATE_PAUSED)
// we use here "and" instead of "&&" because it's binary operation

// will be needed for calculation current player's position
// we only get the spesific position, that was last updated in our player. So, exoplayer won't continuously update the current playback position, instead it will only do it "here and than".
// And with this "here and than" values we can we can now calculate the exact position, actually
inline val PlaybackStateCompat.currentPlaybackPosition: Long
    get() = if(state == PlaybackStateCompat.STATE_PLAYING) {
        // .elapsedRealtime returns the amount of milliseconds since system boot.
        val timeDelta = SystemClock.elapsedRealtime() - lastPositionUpdateTime // Now we have this time difference between the last update time and current time. And now we can calculate actual playback position
        (position + (timeDelta * playbackSpeed)).toLong() // <- what we return
    } else position // <- what we return if else. Position - is the value of milliseconds when the player was last updated