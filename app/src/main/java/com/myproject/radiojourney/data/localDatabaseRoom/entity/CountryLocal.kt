package com.myproject.radiojourney.data.localDatabaseRoom.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

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
)