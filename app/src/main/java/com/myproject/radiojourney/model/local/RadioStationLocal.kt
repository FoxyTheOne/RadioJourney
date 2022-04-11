package com.myproject.radiojourney.model.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.myproject.radiojourney.model.presentation.RadioStationPresentation
import com.myproject.radiojourney.model.remote.RadioStationRemote

@Entity
data class RadioStationLocal(
    @ColumnInfo(name = "stationName") val stationName: String,
    @PrimaryKey
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "clickCount") val clickCount: Int,
    @ColumnInfo(name = "countryCode") val countryCode: String,
    @ColumnInfo(name = "isStationInFavourite") var isStationInFavourite: Boolean
) {

    companion object {
        fun fromRemoteToLocal(
            remote: RadioStationRemote,
            isStationInFavourite: Boolean = false
        ): RadioStationLocal = RadioStationLocal(
            stationName = remote.name,
            url = remote.url,
            clickCount = remote.clickcount,
            countryCode = remote.countrycode,
            isStationInFavourite = isStationInFavourite
        )

        fun fromPresentationToLocal(
            presentation: RadioStationPresentation,
            isStationInFavourite: Boolean = presentation.isStationInFavourite
        ): RadioStationLocal =
            RadioStationLocal(
                stationName = presentation.stationName,
                url = presentation.url,
                clickCount = presentation.clickCount,
                countryCode = presentation.countryCode,
                isStationInFavourite = isStationInFavourite
            )
    }

}