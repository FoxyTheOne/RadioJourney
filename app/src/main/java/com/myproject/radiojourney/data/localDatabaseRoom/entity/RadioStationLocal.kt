package com.myproject.radiojourney.data.localDatabaseRoom.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Станция в таблице Room. @PrimaryKey - uuid станции с сервера.
 *
 * Отдельная модель для базы (local), не та же, что у сервера и у экранов: колонки таблицы меняются реже,
 * чем ответ сервера, и не зависят от того, что показывает экран. Преобразования - в data/mapper/DataMappers.kt
 */
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
)