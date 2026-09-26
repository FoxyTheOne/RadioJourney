package com.myproject.radiojourney.data.localDatabaseRoom.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

/**
 * Страна в таблице Room: код, название, число станций и координаты маркера на карте.
 *
 * Список заполняет фоновая загрузка (CountryCacheWorker) и обновляет целиком, поэтому карта работает без сети.
 * Координаты хранятся как LatLng (класс Google Maps) - его Room не умеет записывать сам, для этого есть LatLngConverter
 */
@Entity
data class CountryLocal(
    @PrimaryKey
    @ColumnInfo(name = "countryCode") val countryCode: String,
    @ColumnInfo(name = "stationcount") val stationcount: Int,
    @ColumnInfo(name = "countryName") val countryName: String,
    @ColumnInfo(name = "countryLocation") val countryLocation: LatLng,
)