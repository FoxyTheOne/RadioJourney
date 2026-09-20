package com.myproject.radiojourney.data.localDatabaseRoom

import androidx.room.*
import com.myproject.radiojourney.data.localDatabaseRoom.entity.RadioStationLocal

// Сравнение через "=", а не LIKE: LIKE - поиск по шаблону (символы % и _ в значении работают как подстановка, регистр не учитывается),
// а здесь нужно точное совпадение id или флага
@Dao
interface IRadioStationDAO {
    @Query("SELECT * from RadioStationLocal WHERE stationuuid = :stationUuid")
    suspend fun getRadioStationByUuid(stationUuid: String): RadioStationLocal?

    // suspend: Room сам выполняет запрос в фоновом потоке. Без suspend вызов из главного потока падает с исключением
    @Query("SELECT * from RadioStationLocal WHERE isStationInFavourite = :isStationInFavorite")
    suspend fun getFavoriteRadioStationList(isStationInFavorite: Boolean): List<RadioStationLocal>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRadioStationIfAbsent(radioStation: RadioStationLocal)

    @Query("UPDATE RadioStationLocal SET isStationInFavourite = :isFavourite WHERE stationuuid = :stationUuid")
    suspend fun updateIsStationInFavourite(stationUuid: String, isFavourite: Boolean)

    // Добавить станцию в избранное или убрать из него.
    // Раньше станция целиком перезаписывалась (REPLACE) данными с экрана, и вместе со звездой затирались другие поля
    // (например, isStationInRecommended), если на экране был устаревший объект. Теперь меняется только флаг избранного;
    // станции, которой ещё нет в базе, сначала добавляется. @Transaction - обе операции выполняются вместе
    @Transaction
    suspend fun setStationFavourite(radioStation: RadioStationLocal, isFavourite: Boolean) {
        insertRadioStationIfAbsent(radioStation.copy(isStationInFavourite = isFavourite))
        updateIsStationInFavourite(radioStation.stationuuid, isFavourite)
    }
}