package com.myproject.radiojourney.entities.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import com.myproject.radiojourney.entities.remote.RadioStationRemote

@Entity
data class RadioStationLocal(
    @PrimaryKey
    @ColumnInfo(name = "stationuuid", defaultValue = "") val stationuuid: String, // Changing PrimaryKey from urlResolved to stationuuid
    @ColumnInfo(name = "url_resolved") val urlResolved: String,
    @ColumnInfo(name = "stationName") val stationName: String,
    @ColumnInfo(name = "clickCount") val clickCount: Int,
    @ColumnInfo(name = "country") val country: String,
    @ColumnInfo(name = "countryCode") val countryCode: String,
    @ColumnInfo(name = "isStationInFavourite") var isStationInFavourite: Boolean,
    @ColumnInfo(name = "isStationInRecommended") var isStationInRecommended: Boolean
) {

    companion object {
        fun fromRemoteToLocal(
            remote: RadioStationRemote,
            isStationInFavourite: Boolean = false,
            isStationInRecommended: Boolean = false
        ): RadioStationLocal = RadioStationLocal(
            stationuuid = remote.stationuuid,
            stationName = remote.name,
            urlResolved = remote.url_resolved,
            clickCount = remote.clickcount,
            country = remote.country,
            countryCode = remote.countrycode,
            isStationInFavourite = isStationInFavourite,
            isStationInRecommended = isStationInRecommended
        )

        fun fromPresentationToLocal(
            presentation: RadioStationPresentation,
            isStationInFavourite: Boolean = presentation.isStationInFavourite,
            isStationInRecommended: Boolean = presentation.isStationInRecommended
        ): RadioStationLocal =
            RadioStationLocal(
                stationuuid = presentation.stationuuid,
                stationName = presentation.stationName,
                urlResolved = presentation.urlResolved,
                clickCount = presentation.clickCount,
                country = presentation.country,
                countryCode = presentation.countryCode,
                isStationInFavourite = isStationInFavourite,
                isStationInRecommended = isStationInRecommended
            )
    }

}