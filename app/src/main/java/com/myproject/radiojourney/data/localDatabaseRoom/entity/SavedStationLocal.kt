package com.myproject.radiojourney.data.localDatabaseRoom.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Станция из последнего удачно скачанного списка страны.
 *
 * Если сервер radio-browser недоступен (или провайдер обрывает соединение с ним), приложение показывает этот список,
 * а не ошибку. Особенно это помогает со станциями своей страны: они обычно вещают с местных серверов
 * и играют, даже когда до самого каталога radio-browser не достучаться.
 *
 * Отдельная таблица, а не RadioStationLocal: там лежат избранные станции, и их нельзя затирать при каждом скачивании списка.
 * Ключ - страна + станция: одна и та же станция может оказаться в списках разных стран
 */
@Entity(primaryKeys = ["countryCode", "stationuuid"])
data class SavedStationLocal(
    // Код страны, чей это список (как его запрашивали: "RU", "DE")
    @ColumnInfo(name = "countryCode") val countryCode: String,
    @ColumnInfo(name = "stationuuid") val stationUuid: String,
    @ColumnInfo(name = "stationName") val stationName: String,
    @ColumnInfo(name = "url_resolved") val urlResolved: String,
    @ColumnInfo(name = "clickCount") val clickCount: Int,
    @ColumnInfo(name = "country") val country: String,
    // Код страны самой станции (как прислал сервер)
    @ColumnInfo(name = "stationCountryCode") val stationCountryCode: String,
    // Место в списке: список показывается в том же порядке, в каком пришёл с сервера
    @ColumnInfo(name = "position") val position: Int,
    // Когда список скачан (System.currentTimeMillis) - одинаково у всех станций списка
    @ColumnInfo(name = "savedAt") val savedAt: Long
)