package com.myproject.radiojourney.other

/**
 * Константы приложения: коды, имена команд сервису плеера и тайминги сетевых запросов.
 *
 * Тайминги собраны в одном месте не для красоты: PROGRESS_TIMEOUT (сколько висит полоса загрузки)
 * должен быть больше, чем максимальное время перебора серверов, иначе полоса пропадёт раньше, чем придёт ответ.
 * Когда такие значения разбросаны по классам, это соотношение легко сломать
 */
object Constants {
    const val MAX_STATIONS_COUNT = 300

    // Сколько самых популярных станций запросить, если полный список страны обрывается на середине (ServerError.CONNECTION_CUT).
    // В некоторых сетях с сервера radio-browser проходят только первые ~16 КБ ответа. Одна станция - около 1,2 КБ,
    // 10 станций - около 11 КБ: такой ответ укладывается, и пользователь получает хотя бы самые популярные станции
    const val POPULAR_STATIONS_FALLBACK_COUNT = 10
    const val DEFAULT_COUNTRY_CODE = "AQ"

    // Служебные "коды страны" плейлистов, которых нет на карте:
    // FAV - избранное (к коду страны станции добавляется суффикс _FAV), MY - свои станции пользователя
    const val FAVOURITES_COUNTRY_CODE = "FAV"
    const val FAVOURITES_COUNTRY_CODE_SUFFIX = "_FAV"
    const val MY_STATIONS_COUNTRY_CODE = "MY"
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

    // Запросы к серверу radio-browser (RadioServiceWrapper, NetworkRadioDataSource).
    // Все константы, которые влияют друг на друга по времени, собраны здесь, чтобы их можно было сравнить глазами
    const val NETWORK_CONNECT_TIMEOUT = 5_000L // подключение к одному адресу сервера
    const val NETWORK_READ_TIMEOUT = 10_000L // пауза в получении данных
    const val NETWORK_CALL_TIMEOUT = 25_000L // один запрос целиком
    const val SERVER_SEARCH_TIME = 35_000L // сколько времени перебираем серверы, прежде чем сдаться
    const val SERVER_RETRY_DELAY = 1_000L // пауза перед попыткой на следующем сервере
    const val DNS_RETRY_DELAY = 1_000L // пауза перед повторным DNS-запросом списка серверов
    const val DNS_ATTEMPTS =
        3 // сколько раз спрашиваем у DNS список серверов, прежде чем взять запасной
    const val DNS_SERVER_LIST_NAME =
        "all.api.radio-browser.info" // имя, по которому DNS отдаёт все серверы radio-browser
    const val FALLBACK_SERVER =
        "de1.api.radio-browser.info" // сервер из документации API - на случай, если DNS не ответил

    // Сколько максимум может висеть полоса загрузки плейлиста/станции. Потом прячем её и показываем ошибку.
    // Должно быть больше, чем может занять перебор серверов, иначе полоса пропадёт раньше, чем придёт плейлист:
    // DNS (DNS_ATTEMPTS попыток с паузой DNS_RETRY_DELAY) + перебор серверов (SERVER_SEARCH_TIME)
    // + последний начатый запрос, который успел стартовать до конца перебора (NETWORK_CALL_TIMEOUT) + запас
    const val PROGRESS_TIMEOUT =
        DNS_ATTEMPTS * DNS_RETRY_DELAY + SERVER_SEARCH_TIME + NETWORK_CALL_TIMEOUT + 10_000L

    // Через сколько убрать уведомление, если радио стоит на паузе (или остановлено ошибкой)
    const val PAUSED_NOTIFICATION_TIMEOUT = 5 * 60_000L
}