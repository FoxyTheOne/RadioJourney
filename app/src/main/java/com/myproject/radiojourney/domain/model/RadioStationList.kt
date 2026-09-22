package com.myproject.radiojourney.domain.model

/**
 * Список станций страны и то, откуда он взялся.
 *
 * Если сервер radio-browser недоступен, репозиторий отдаёт список, сохранённый в телефоне при прошлом удачном скачивании,
 * а если сохранённого нет и ответ сервера обрывается - только несколько самых популярных станций.
 * Экрану нужно об этом знать, чтобы сказать пользователю, что список не свежий или неполный
 *
 * @param savedAt когда список был сохранён (System.currentTimeMillis). null - список только что пришёл с сервера
 * @param isOnlyPopular в списке только самые популярные станции (POPULAR_STATIONS_FALLBACK_COUNT): полный список
 * не удалось скачать, потому что ответ сервера обрывался на середине
 */
data class RadioStationList(
    val stations: List<RadioStation>,
    val savedAt: Long? = null,
    val isOnlyPopular: Boolean = false
) {
    val isSaved: Boolean
        get() = savedAt != null

    // Список не полный и свежий, а запасной - пользователю стоит об этом сказать
    val isFallback: Boolean
        get() = isSaved || isOnlyPopular
}