package com.myproject.radiojourney.model.remote

import com.google.gson.annotations.SerializedName

/*
Copyright (c) 2022 Kotlin Data Classes Generated from JSON powered by http://www.json2kotlin.com

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

For support, please feel free to contact me at https://www.linkedin.com/in/syedabsar */

data class RadioStationRemote(
    @SerializedName("changeuuid") val changeuuid: String,
    @SerializedName("stationuuid") val stationuuid: String,
    @SerializedName("serveruuid") val serveruuid: String,
    @SerializedName("name") val name: String,
    @SerializedName("url") val url: String,
    @SerializedName("url_resolved") val url_resolved: String,
    @SerializedName("homepage") val homepage: String,
    @SerializedName("favicon") val favicon: String,
    @SerializedName("tags") val tags: String,
    @SerializedName("country") val country: String,
    @SerializedName("countrycode") val countrycode: String,
    @SerializedName("iso_3166_2") val iso_3166_2: String,
    @SerializedName("state") val state: String,
    @SerializedName("language") val language: String,
    @SerializedName("languagecodes") val languagecodes: String,
    @SerializedName("votes") val votes: Int,
    @SerializedName("lastchangetime") val lastchangetime: String,
    @SerializedName("lastchangetime_iso8601") val lastchangetime_iso8601: String,
    @SerializedName("codec") val codec: String,
    @SerializedName("bitrate") val bitrate: Int,
    @SerializedName("hls") val hls: Int,
    @SerializedName("lastcheckok") val lastcheckok: Int,
    @SerializedName("lastchecktime") val lastchecktime: String,
    @SerializedName("lastchecktime_iso8601") val lastchecktime_iso8601: String,
    @SerializedName("lastcheckoktime") val lastcheckoktime: String,
    @SerializedName("lastcheckoktime_iso8601") val lastcheckoktime_iso8601: String,
    @SerializedName("lastlocalchecktime") val lastlocalchecktime: String,
    @SerializedName("lastlocalchecktime_iso8601") val lastlocalchecktime_iso8601: String,
    @SerializedName("clicktimestamp") val clicktimestamp: String,
    @SerializedName("clicktimestamp_iso8601") val clicktimestamp_iso8601: String,
    @SerializedName("clickcount") val clickcount: Int,
    @SerializedName("clicktrend") val clicktrend: Int,
    @SerializedName("ssl_error") val ssl_error: Int,
    @SerializedName("geo_lat") val geo_lat: String,
    @SerializedName("geo_long") val geo_long: String,
    @SerializedName("has_extended_info") val has_extended_info: Boolean
)