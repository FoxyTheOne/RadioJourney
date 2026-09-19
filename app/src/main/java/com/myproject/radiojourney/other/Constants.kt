package com.myproject.radiojourney.other

object Constants {
    const val MAX_STATIONS_COUNT = 300
    const val DEFAULT_COUNTRY_CODE = "AQ"
    const val MEDIA_ROOT_ID = "root_id"
    const val NETWORK_ERROR = "NETWORK_ERROR"

    // Команды сервису плеера (MusicLibrarySessionCallback.onCustomCommand)
    const val ADD_SONGS = "Add Songs"
    const val CANCEL_PLAYLIST_DOWNLOAD =
        "Cancel playlist download" // отменить загрузку плейлиста (по таймауту полосы загрузки)
    const val COUNTRY_CODE_ID = "Country code"

    // Notification from exoplayer
    const val NOTIFICATION_CHANNEL_ID = "music"
    const val NOTIFICATION_ID = 1

    const val SERVER_IS_DOWN = "SERVER_IS_DOWN"

    // Запросы к серверу radio-browser (RadioServiceWrapper, NetworkRadioDataSource)
    const val NETWORK_CONNECT_TIMEOUT = 5_000L // подключение к одному адресу сервера
    const val NETWORK_READ_TIMEOUT = 15_000L // пауза в получении данных
    const val NETWORK_CALL_TIMEOUT = 30_000L // один запрос целиком
    const val SERVER_SEARCH_TIME = 40_000L // сколько времени перебираем серверы, прежде чем сдаться

    // Сколько максимум может висеть полоса загрузки плейлиста/станции. Потом прячем её и показываем ошибку.
    // Должно быть больше, чем может занять перебор серверов: SERVER_SEARCH_TIME + последний запрос (NETWORK_CALL_TIMEOUT) + запас
    const val PROGRESS_TIMEOUT = SERVER_SEARCH_TIME + NETWORK_CALL_TIMEOUT + 20_000L

    // Через сколько убрать уведомление, если радио стоит на паузе (или остановлено ошибкой)
    const val PAUSED_NOTIFICATION_TIMEOUT = 5 * 60_000L
}