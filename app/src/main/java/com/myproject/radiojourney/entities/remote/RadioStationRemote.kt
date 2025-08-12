package com.myproject.radiojourney.entities.remote

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
    @SerializedName("url_resolved") var url_resolved: String = "",
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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RadioStationRemote

        if (changeuuid != other.changeuuid) return false
        if (stationuuid != other.stationuuid) return false
        if (serveruuid != other.serveruuid) return false
        if (name != other.name) return false
        if (url != other.url) return false
        if (url_resolved != other.url_resolved) return false
        if (homepage != other.homepage) return false
        if (favicon != other.favicon) return false
        if (tags != other.tags) return false
        if (country != other.country) return false
        if (countrycode != other.countrycode) return false
        if (iso_3166_2 != other.iso_3166_2) return false
        if (state != other.state) return false
        if (language != other.language) return false
        if (languagecodes != other.languagecodes) return false
        if (votes != other.votes) return false
        if (lastchangetime != other.lastchangetime) return false
        if (lastchangetime_iso8601 != other.lastchangetime_iso8601) return false
        if (codec != other.codec) return false
        if (bitrate != other.bitrate) return false
        if (hls != other.hls) return false
        if (lastcheckok != other.lastcheckok) return false
        if (lastchecktime != other.lastchecktime) return false
        if (lastchecktime_iso8601 != other.lastchecktime_iso8601) return false
        if (lastcheckoktime != other.lastcheckoktime) return false
        if (lastcheckoktime_iso8601 != other.lastcheckoktime_iso8601) return false
        if (lastlocalchecktime != other.lastlocalchecktime) return false
        if (lastlocalchecktime_iso8601 != other.lastlocalchecktime_iso8601) return false
        if (clicktimestamp != other.clicktimestamp) return false
        if (clicktimestamp_iso8601 != other.clicktimestamp_iso8601) return false
        if (clickcount != other.clickcount) return false
        if (clicktrend != other.clicktrend) return false
        if (ssl_error != other.ssl_error) return false
        if (geo_lat != other.geo_lat) return false
        if (geo_long != other.geo_long) return false
        if (has_extended_info != other.has_extended_info) return false

        return true
    }

    override fun hashCode(): Int {
        var result = changeuuid.hashCode()
        result = 31 * result + stationuuid.hashCode()
        result = 31 * result + serveruuid.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + url_resolved.hashCode()
        result = 31 * result + homepage.hashCode()
        result = 31 * result + favicon.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + country.hashCode()
        result = 31 * result + countrycode.hashCode()
        result = 31 * result + iso_3166_2.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + language.hashCode()
        result = 31 * result + languagecodes.hashCode()
        result = 31 * result + votes
        result = 31 * result + lastchangetime.hashCode()
        result = 31 * result + lastchangetime_iso8601.hashCode()
        result = 31 * result + codec.hashCode()
        result = 31 * result + bitrate
        result = 31 * result + hls
        result = 31 * result + lastcheckok
        result = 31 * result + lastchecktime.hashCode()
        result = 31 * result + lastchecktime_iso8601.hashCode()
        result = 31 * result + lastcheckoktime.hashCode()
        result = 31 * result + lastcheckoktime_iso8601.hashCode()
        result = 31 * result + lastlocalchecktime.hashCode()
        result = 31 * result + lastlocalchecktime_iso8601.hashCode()
        result = 31 * result + clicktimestamp.hashCode()
        result = 31 * result + clicktimestamp_iso8601.hashCode()
        result = 31 * result + clickcount
        result = 31 * result + clicktrend
        result = 31 * result + ssl_error
        result = 31 * result + geo_lat.hashCode()
        result = 31 * result + geo_long.hashCode()
        result = 31 * result + has_extended_info.hashCode()
        return result
    }

    override fun toString(): String {
        return "RadioStationRemote(changeuuid='$changeuuid', stationuuid='$stationuuid', serveruuid='$serveruuid', name='$name', url='$url', url_resolved='$url_resolved', homepage='$homepage', favicon='$favicon', tags='$tags', country='$country', countrycode='$countrycode', iso_3166_2='$iso_3166_2', state='$state', language='$language', languagecodes='$languagecodes', votes=$votes, lastchangetime='$lastchangetime', lastchangetime_iso8601='$lastchangetime_iso8601', codec='$codec', bitrate=$bitrate, hls=$hls, lastcheckok=$lastcheckok, lastchecktime='$lastchecktime', lastchecktime_iso8601='$lastchecktime_iso8601', lastcheckoktime='$lastcheckoktime', lastcheckoktime_iso8601='$lastcheckoktime_iso8601', lastlocalchecktime='$lastlocalchecktime', lastlocalchecktime_iso8601='$lastlocalchecktime_iso8601', clicktimestamp='$clicktimestamp', clicktimestamp_iso8601='$clicktimestamp_iso8601', clickcount=$clickcount, clicktrend=$clicktrend, ssl_error=$ssl_error, geo_lat='$geo_lat', geo_long='$geo_long', has_extended_info=$has_extended_info)"
    }
}