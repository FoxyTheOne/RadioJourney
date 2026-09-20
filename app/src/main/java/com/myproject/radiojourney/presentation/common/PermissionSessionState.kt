package com.myproject.radiojourney.presentation.common

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Какие разрешения приложение уже запрашивало за этот запуск.
 *
 * Нужен, чтобы объяснение ("зачем нужно местоположение") показывалось один раз за сеанс, а не при каждом
 * открытии карты: экраны создаются заново при каждом переходе и при повороте, и запрос повторялся бы снова и снова.
 *
 * @Singleton здесь и значит "на один запуск приложения": объект живёт, пока живёт процесс,
 * и исчезает вместе с ним. Хранить это в настройках (DataStore) не нужно - при следующем запуске
 * приложение имеет право спросить ещё раз
 */
@Singleton
class PermissionSessionState @Inject constructor() {

    private val requestedPermissions = mutableSetOf<String>()

    // Первый запрос этого разрешения за сеанс? Заодно запоминает, что запрос был.
    // add() у множества возвращает true, только если элемента там ещё не было - то, что нам и нужно
    fun isFirstRequestInSession(permission: String): Boolean = requestedPermissions.add(permission)

    // Это разрешение в текущем сеансе уже запрашивали (и пользователь отказал - иначе бы мы сюда не попали)
    fun wasRequestedInSession(permission: String): Boolean = permission in requestedPermissions
}
