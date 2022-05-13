package com.myproject.radiojourney.utils.musicPlayer

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.myproject.radiojourney.other.Constants
import com.myproject.radiojourney.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForegroundNotificationService : Service() {
    private var musicNotification: NotificationCompat.Builder? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        val musicNotificationIcon: Bitmap = BitmapFactory.decodeResource(
            this.resources,
            R.drawable.radio_clker_free_vector_images_pixabay
        )

        // Вызываем метод для создания Channel
        // Добавляем проверку, т.к. создавать NotificationChannel можно только начиная с API 26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createMusicNotificationChannel()
        }

        // Channel создан, теперь можно приступить непосредственно к созданию уведомления
        // 1. Создаём notification c помощью билдера, который первым будет показываться нашему пользователю
        musicNotification =
            NotificationCompat.Builder(this, Constants.NOTIFICATION_MUSIC_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note_orange)
                .setContentTitle(getString(R.string.homeRadio_selectRadioStation))
                .setContentText(getString(R.string.homeRadio_selectRadioStation))
                .setLargeIcon(musicNotificationIcon)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)

        // 2. Для того, чтобы Service из обычного перешел в Foreground, нам нужно вызвать метод startForeground() внутри этого сервиса
        startForeground(1, musicNotification?.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    // START_STICKY:
    // If this service's process is killed while it is started (after returning from onStartCommand(Intent, int, int)),
    // then leave it in the started state but don't retain this delivered intent. Later the system will try to re-create the service.
    // Because it is in the started state, it will guarantee to call onStartCommand(Intent, int, int) after creating the new service instance;
    // if there are not any pending start commands to be delivered to the service, it will be called with a null intent object,
    // so you must take care to check for this.
    //
    // This mode makes sense for things that will be explicitly started and stopped to run for arbitrary periods of time,
    // such as a service performing background music playback.
    //
    // START_NOT_STICKY:
    // If this service's process is killed while it is started (after returning from onStartCommand(Intent, int, int)),
    // and there are no new start intents to deliver to it, then take the service out of the started state and don't recreate
    // until a future explicit call to Context.startService(Intent). The service will not receive a onStartCommand(Intent, int, int) call
    // with a null Intent because it will not be re-started if there are no pending Intents to deliver.
    //
    // This mode makes sense for things that want to do some work as a result of being started, but can be stopped when under memory pressure
    // and will explicit start themselves again later to do more work. An example of such a service would be one that polls for data from a server:
    // it could schedule an alarm to poll every N minutes by having the alarm start its service. When its onStartCommand(Intent, int, int) is called
    // from the alarm, it schedules a new alarm for N minutes later, and spawns a thread to do its networking.
    // If its process is killed while doing that check, the service will not be restarted until the alarm goes off.

    // FOREGROUND_SERVICE -> 2. Создадим Channel CashingCountries
    // MUSIC PLAYER ON NOTIFICATION -> 2. Create a channel for the notification
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createMusicNotificationChannel() {
        // 2.1. Создаём Channel и регистрируем его
        val musicNotificationChannel = NotificationChannel(
            Constants.NOTIFICATION_MUSIC_CHANNEL_ID,
            "RadioStationPlaying",
            NotificationManager.IMPORTANCE_LOW
        )
        // 2.4. Находим NotificationManager
        val notificationManager =
            this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // 2.5. И вызываем у него метод createNotificationChannel(), куда передаём наш channel
        notificationManager.createNotificationChannel(musicNotificationChannel)
    }
}