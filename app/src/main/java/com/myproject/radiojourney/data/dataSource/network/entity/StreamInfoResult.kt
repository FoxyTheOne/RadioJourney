package com.myproject.radiojourney.data.dataSource.network.entity

import com.google.gson.annotations.SerializedName

data class StreamInfoResult(
    @SerializedName("ok") var ok: String? = null,
    @SerializedName("message") var message: String? = null,
    @SerializedName("stationuuid") var stationuuid: String? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("url") var url: String? = null
)