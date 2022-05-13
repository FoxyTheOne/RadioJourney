package com.myproject.radiojourney.other

object Constants {
    // К каждой группе NOTIFICATION мы создаём свой CHANNEL_ID
    const val NOTIFICATION_MUSIC_CHANNEL_ID = "CHANNEL_ID"
    // Создаём необходимое количество Actions:
    const val NOTIFICATION_MUSIC_ACTION_PLAY = "CHANNEL_PLAY"

    // Broadcast
    const val NOTIFICATION_MUSIC_ACTION_BROADCAST = "TRACKS_TRACKS"
    const val MUSIC_PLAYER_SERVICE_FAILURE_PLAYING_BROADCAST = "FAILURE_PLAYING"

    // Notification from exoplayer
    const val NOTIFICATION_CHANNEL_ID = "music"
    const val NOTIFICATION_ID = 1

    const val MEDIA_ROOT_ID = "root_id"

    const val NETWORK_ERROR = "NETWORK_ERROR"
}