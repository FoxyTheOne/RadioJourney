package com.myproject.radiojourney.utils.musicPlayer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.myproject.radiojourney.Constants
import com.myproject.radiojourney.R
import com.myproject.radiojourney.model.presentation.RadioStationPresentation
import java.util.*

/**
 * Создадим Notification для управления музыкой
 *
 * MUSIC PLAYER ON NOTIFICATION -> 1. Create a class with static method (CreateNotification) + add a library
 * MUSIC PLAYER ON NOTIFICATION -> 2. ForegroundNotificationService: create a channel for the notification. HomeRadioFragment: onDestroy - cancel all notifications
 * MUSIC PLAYER ON NOTIFICATION -> 3. Create Service (MusicPlayerBoundService). Create there notification in methods. Called when play is play is pressed, for instance
 * MUSIC PLAYER ON NOTIFICATION -> 4. Look further (down here)
 * MUSIC PLAYER ON NOTIFICATION -> 5. Fill MusicPlayerBoundService logic (MediaPlayer stop/play) etc.
 * MUSIC PLAYER ON NOTIFICATION -> 6. Create Interface (IPlayable)
 * MUSIC PLAYER ON NOTIFICATION -> 7. HomeRadioFragment. Implements IPlayable, where it is needed
 * MUSIC PLAYER ON NOTIFICATION -> 8. HomeRadioFragment. Receiving Broadcast and call methods from IPlayable, which will call methods from MusicPlayerBoundService and change notification and fragment image and text
 * MUSIC PLAYER ON NOTIFICATION -> END. HomeRadioFragment. Unbind service, unregister broadcast, cancel all notifications
 */
class CreateNotification {
    companion object {
        fun updateNotification(
            context: Context,
            radioStation: RadioStationPresentation,
            playButton: Int
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManagerCompat =
                    NotificationManagerCompat.from(context)
                val mediaSessionCompat =
                    MediaSessionCompat(context, "tagMediaSessionCompat")

//                ContextCompat.getDrawable(context, R.drawable.radio)?.toBitmap()
//                val icon: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.radio_blende12_pixabay)
//                val icon: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.kasset_ansfoto_pixabay)
//                val icon: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.radio_heissenstein_pixabay) // +
//                val icon: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.radio_stock_snap_pixabay_cut) // +
                val icon: Bitmap = BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.radio_clker_free_vector_images_pixabay
                ) // +

                // Узнаем название страны
                val loc = Locale("", radioStation.countryCode)
                val countryName = loc.displayName

                // MUSIC PLAYER ON NOTIFICATION -> 4. Create broadcast class (NotificationActionBroadcast),
                // Create all needed Intents + add ".addAction(playButton, "Play", pendingIntentPlay)" in notification builder
                // Add also ".setStyle"
                val intentPlay = Intent(context, NotificationActionBroadcast::class.java)
                    .setAction(Constants.NOTIFICATION_MUSIC_ACTION_PLAY)
                val pendingIntentPlay = PendingIntent.getBroadcast(
                    context,
                    0,
                    intentPlay,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                // create a Notification
                val notification =
                    NotificationCompat.Builder(context, Constants.NOTIFICATION_MUSIC_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_music_note_orange)
                        .setContentTitle(radioStation.stationName)
                        .setContentText(countryName)
                        .setLargeIcon(icon)
                        .setOnlyAlertOnce(true)
                        .setShowWhen(false)
                        .addAction(playButton, "Play", pendingIntentPlay)
                        .setStyle(
                            androidx.media.app.NotificationCompat.MediaStyle()
                                .setShowActionsInCompactView(0)
                                .setMediaSession(mediaSessionCompat.sessionToken)
                        )
                        .setPriority(NotificationCompat.PRIORITY_LOW)

                notificationManagerCompat.notify(1, notification.build())
            }
        }
    }
}