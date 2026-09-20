package com.myproject.radiojourney.data.localDatabaseRoom.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Станция, которую пользователь добавил сам, по ссылке на поток ("Мои радиостанции").
 *
 * Отдельная таблица, а не RadioStationLocal с флагом: у своей станции нет ничего из того, что приходит с сервера
 * (кода страны, числа прослушиваний, uuid из каталога), и её не должен затирать список, скачанный с radio-browser.
 *
 * stationUuid генерируется самим приложением (UUID.randomUUID) - он нужен плееру, чтобы отличать станции друг от друга
 */
@Entity
data class MyStationLocal(
    @PrimaryKey
    @ColumnInfo(name = "stationuuid") val stationUuid: String,
    @ColumnInfo(name = "stationName") val stationName: String,
    @ColumnInfo(name = "url_resolved") val urlResolved: String,
    // Время добавления: список показывается в том порядке, в котором пользователь его собирал
    @ColumnInfo(name = "addedAt") val addedAt: Long
)