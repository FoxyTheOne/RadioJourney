package com.myproject.radiojourney.utils.exoplayer.callback

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.myproject.radiojourney.other.Constants.NOTIFICATION_ID
import com.myproject.radiojourney.utils.exoplayer.MusicService

class MusicPlayerNotificationListener(
    private val musicService: MusicService
) : PlayerNotificationManager.NotificationListener {

    // what we will do when notification is cancelled
    override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
        musicService.apply {
            // stopForeground(true) - deprecated
            // STOP_FOREGROUND_DETACH if set, the notification previously supplied to startForeground(int, Notification) will be detached from the service's lifecycle.
            // The notification will remain shown even after the service is stopped and destroyed.
            // STOP_FOREGROUND_REMOVE if supplied, the notification previously supplied to startForeground(int, Notification) will be cancelled and removed from display.
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(Service.STOP_FOREGROUND_REMOVE)
            } else {
                stopForeground(true)
            }

            isForegroundService = false
            stopSelf()
        }

        super.onNotificationCancelled(notificationId, dismissedByUser)
    }

    override fun onNotificationPosted(
        notificationId: Int,
        notification: Notification,
        ongoing: Boolean
    ) {
        musicService.apply {
            if (ongoing && !isForegroundService) {
                ContextCompat.startForegroundService(
                    this,
                    Intent(applicationContext, this::class.java)
                )
                startForeground(NOTIFICATION_ID, notification)
                isForegroundService = true
            }
        }

        super.onNotificationPosted(notificationId, notification, ongoing)
    }
}