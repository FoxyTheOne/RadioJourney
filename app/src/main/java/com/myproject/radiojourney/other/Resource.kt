package com.myproject.radiojourney.other

/**
 * A general class, that can be reused in other projects
 *
 * We can check if the data was loaded correctly
 * So in fragment we just check if the Resource got success, loading or error status status
 * and according to it do what we need to do there
 */
// "out T" means we need numbers (T type is a number)
data class Resource<out T>(val status: Status, val data: T?, val message: String?) {

    // functions for corresponding status from enum class Status
    companion object {
        fun <T> success(data: T?) = Resource(Status.SUCCESS, data, null)

        fun <T> error(message: String, data: T?) = Resource(Status.ERROR, data, message)

        // when you implement a cashing mehanizm, you could already have a data that comes from the cash while you load the data that comes from the remote data source
        fun <T> loading(data: T?) = Resource(Status.LOADING, data, null)
    }
}

enum class Status {
    SUCCESS,
    ERROR,
    LOADING
}