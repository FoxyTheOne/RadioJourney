package com.myproject.radiojourney.domain.model

/**
 * Список станций страны и то, откуда он взялся.
 *
 * Если сервер radio-browser недоступен, репозиторий отдаёт список, сохранённый в телефоне при прошлом удачном скачивании.
 * Экрану нужно об этом знать, чтобы сказать пользователю, что список не свежий
 *
 * @param savedAt когда список был сохранён (System.currentTimeMillis). null - список только что пришёл с сервера
 */
data class RadioStationList(
    val stations: List<RadioStation>,
    val savedAt: Long? = null
) {
    val isSaved: Boolean
        get() = savedAt != null
}