package com.myproject.radiojourney.other

/**
 * A general class, that can be reused in other projects
 *
 * Triggers a specific event
 * Once we trigger an event, we will set var hasBeenHandled to true and then it just won't emmit this event again and will just emmit null
 * It will be useful for error messages, for instance
 */
open class Event<out T>(private val data: T) {

    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            data
        }
    }

    // if you need to pick up data, even if it has already been handled
    fun peekContent() = data
}