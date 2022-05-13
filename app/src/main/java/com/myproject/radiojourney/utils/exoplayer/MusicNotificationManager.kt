package com.myproject.radiojourney.utils.exoplayer

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.myproject.radiojourney.R
import com.myproject.radiojourney.other.Constants.NOTIFICATION_CHANNEL_ID
import com.myproject.radiojourney.other.Constants.NOTIFICATION_ID

class MusicNotificationManager(
    private val context: Context,
    sessionToken: MediaSessionCompat.Token,
    notificationListener: PlayerNotificationManager.NotificationListener,
    private val newSongCallback: () -> Unit
) {

    private val notificationManager: PlayerNotificationManager // custom class from exoplayer for a notification

    init {
        val mediaController = MediaControllerCompat(context, sessionToken)

        // createWithNotificationChannel() function creates a notification channel for us
        notificationManager = PlayerNotificationManager.createWithNotificationChannel(
            context,
            NOTIFICATION_CHANNEL_ID,
            R.string.notification_channel_name,
            R.string.notification_channel_description,
            NOTIFICATION_ID,
            DescriptionAdapter(mediaController),
            notificationListener
        ).apply {
            setSmallIcon(R.drawable.ic_music_note_orange)
            setMediaSessionToken(sessionToken) // gives our notification manager access to our current media session in our music service. So, it will se changes in our music service
        }
    }

    // At last, let's create a function for our exoplayer notification
    fun showNotification(player: Player) {
        notificationManager.setPlayer(player)
    }

    private inner class DescriptionAdapter(
        private val mediaController: MediaControllerCompat
    ) : PlayerNotificationManager.MediaDescriptionAdapter {

        // here we just return the title of the song
        override fun getCurrentContentTitle(player: Player): CharSequence {
            return mediaController.metadata.description.title.toString()
        }

        // intent, that we already described in music service
        override fun createCurrentContentIntent(player: Player): PendingIntent? {
            return mediaController.sessionActivity
        }

        // here we just return our subtitle
        override fun getCurrentContentText(player: Player): CharSequence? {
            return mediaController.metadata.description.subtitle.toString()
        }

        // Our large icon for notification
        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {

            return BitmapFactory.decodeResource(
                context.resources,
                R.drawable.radio_clker_free_vector_images_pixabay
            )

            // In case we download image on every song:
//            Glide.with(context).asBitmap()
//                .load(mediaController.metadata.description.iconUri)
//                .into(object : CustomTarget<Bitmap>() {
//                    override fun onResourceReady(
//                        resource: Bitmap,
//                        transition: Transition<in Bitmap>?
//                    ) {
//                        callback.onBitmap(resource)
//                    }
//
//                    override fun onLoadCleared(placeholder: Drawable?) = Unit
//                })
//            return null
            // We can return null, because it takes time to download an image. We just call a callback function, when it is ready.

        }
    }
}