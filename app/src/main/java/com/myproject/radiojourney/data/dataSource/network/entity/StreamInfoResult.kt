package com.myproject.radiojourney.data.dataSource.network.entity

import com.google.gson.annotations.SerializedName

/**
 * Ответ сервера на запрос /json/url/{uuid} - отметку "станцию слушают" (её просит делать автор API).
 * Поле ok сообщает, что сервер принял отметку
 */
data class StreamInfoResult(
    @SerializedName("ok") var ok: String? = null,
    @SerializedName("message") var message: String? = null,
    @SerializedName("stationuuid") var stationuuid: String? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("url") var url: String? = null
)