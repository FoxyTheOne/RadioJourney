package com.myproject.radiojourney.entities.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng
import com.myproject.radiojourney.entities.remote.CountryCodeRemote

/**
 * Класс для сохранения в базе данных Room локаций
 */
@Entity
data class CountryLocal(
    @PrimaryKey
    @ColumnInfo(name = "countryCode") val countryCode: String,
    @ColumnInfo(name = "stationcount") val stationcount: Int,
    @ColumnInfo(name = "countryName") val countryName: String,
    @ColumnInfo(name = "countryLocation") val countryLocation: LatLng,
) {

    companion object {
        fun fromRemoteToLocal(
            remote: CountryCodeRemote,
            countryName: String,
            countryLocation: LatLng
        ): CountryLocal = CountryLocal(
            countryCode = remote.name,
            stationcount = remote.stationcount,
            countryName = countryName,
            countryLocation = countryLocation
        )
    }

}