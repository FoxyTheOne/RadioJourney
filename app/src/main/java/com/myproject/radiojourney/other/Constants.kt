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

    const val UPDATE_PLAYER_POSITION_INTERVAL =
        100L // we will update our seek bar 10 times a second

    const val ADD_SONGS = "Add Songs"

    const val AUDIO_CONNECTING = "Connecting to radio station..."
    const val AUDIO_PLAYING = "Audio started playing"
    const val AUDIO_STOPPED = "Audio stopped"

    // Broadcast для полосы прогресса FirstScreenLoadingFragment - ProgressForegroundService
    const val FILTER_FOR_BROADCAST = "FILTER_FOR_BROADCAST"
    const val KEY_BROADCAST_LIST_SIZE = "KEY_BROADCAST_LIST_SIZE"
    const val KEY_BROADCAST_COUNT = "KEY_BROADCAST_COUNT"
    const val KEY_BROADCAST_END = "KEY_BROADCAST_END"

    // Broadcast для полосы прогресса MainActivity при загрузке плейлиста
    const val FILTER_FOR_BROADCAST_MA = "FILTER_FOR_BROADCAST_MA"
    const val KEY_BROADCAST_LIST_SIZE_MA = "KEY_BROADCAST_LIST_SIZE_MA"
    const val KEY_BROADCAST_COUNT_MA = "KEY_BROADCAST_COUNT_MA"
    const val KEY_BROADCAST_END_MA = "KEY_BROADCAST_END_MA"
}