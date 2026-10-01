package com.myproject.radiojourney.other

/**
 * Ответ на запрос данных: получилось (SUCCESS) или не получилось (ERROR).
 *
 * Обёртка нужна, чтобы вместе с данными передать и неудачу, не бросая исключение: в data слое запрос к серверу -
 * дело обычное, и неудача - это не ошибка программы, а один из ответов, который экран должен показать пользователю.
 * В message при ошибке лежит причина - имя значения ServerError.
 *
 * Переиспользование: класс не зависит ни от чего в этом проекте, его можно забрать в любой другой
 */
// "out T" means we need numbers (T type is a number)
data class Resource<out T>(val status: Status, val data: T?, val message: String?) {

    companion object {
        fun <T> success(data: T?) = Resource(Status.SUCCESS, data, null)

        fun <T> error(message: String, data: T?) = Resource(Status.ERROR, data, message)
    }
}

// Раньше было ещё LOADING ("данные ещё грузятся") - его никто не использовал: загрузку экраны показывают
// через собственные состояния (например, MainViewModel.LoadingState)
enum class Status {
    SUCCESS,
    ERROR
}