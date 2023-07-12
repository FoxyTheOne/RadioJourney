package com.myproject.radiojourney.utils.exoplayer.callback

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.myproject.radiojourney.other.Constants.NOTIFICATION_ID
import com.myproject.radiojourney.presentation.MainViewModel
import com.myproject.radiojourney.utils.exoplayer.MusicService

class MusicPlayerNotificationListener(
    private val musicService: MusicService
) : PlayerNotificationManager.NotificationListener {

    companion object {
        private const val TAG = "MPNotificationListener"
    }

    // what we will do when notification is cancelled
    override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
        Log.d(
            TAG,
            "Вызван метод onNotificationCancelled() в классе MusicPlayerNotificationListener"
        )

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
//        musicService.apply {
//            if (ongoing && !isForegroundService) {
//                ContextCompat.startForegroundService(
//                    this,
//                    Intent(applicationContext, this::class.java)
//                )
//                startForeground(NOTIFICATION_ID, notification)
//                isForegroundService = true
//            }
//        }
//
//        super.onNotificationPosted(notificationId, notification, ongoing)

        // Уведомление не пропадает. И его нельзя закрыть. Исправляем:
        musicService.apply {
            if (ongoing) {

                if (!isForegroundService) {
                    ContextCompat.startForegroundService(
                        this,
                        Intent(applicationContext, this::class.java)
                    )
                    startForeground(NOTIFICATION_ID, notification)
                    isForegroundService = true
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

            } else {

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

        super.onNotificationPosted(notificationId, notification, ongoing)
    }

    /** То, что мне помогло (уведомление нельзя было закрыть):
     *
     * The notification can only be swiped when not assigned to a foreground service. So it requires you to stop the foreground service, when the notification is not ongoing anymore (that is when the player is paused).
     * You are pretty close to that already with the code you show above. You need to change onNotificationPosted() and check wether the notification is still ongoing. If paused isOngoing is false and you should stop the foreground service. Now the notification can be swiped, because it's not tied to a foreground service anymore.
     * When you receive the cancellation event after swipe, you can totally destroy your service. The user needs to restart in the app UI without notification. That's when you can start the cycle again and start your foreground service again.
     * That could probably look like this:
     *
     * @Override
     * public void onNotificationPosted(
     * int notificationId, Notification notification, boolean ongoing) {
     *  if (ongoing) {
     *   startForeground(notificationId, notification);
     *  } else {
     *   stopForeground(/* removeNotification= */ false);
     *  }
     * }
     *
     * @Override
     * public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
     *  stopSelf();
     * }
     */

}