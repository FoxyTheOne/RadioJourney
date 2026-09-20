package com.myproject.radiojourney.data.localDatabaseRoom

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.myproject.radiojourney.data.localDatabaseRoom.entity.CountryLocal
import kotlinx.coroutines.flow.Flow

/**
 * DAO стран: Room сам пишет реализацию по этим аннотациям.
 *
 * Полезные приёмы, которые можно забрать в другой проект:
 * - suspend-методы: Room выполняет их в фоновом потоке и ругается на обращение к базе из главного;
 * - Flow: подписка на таблицу, новое значение приходит само после каждой записи;
 * - @Transaction над обычным методом: несколько операций выполняются как одна (подписчики не увидят промежуточное состояние)
 */
@Dao
interface ICountryDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCountryList(countryLocalList: List<CountryLocal>)

    @Query("DELETE FROM CountryLocal")
    suspend fun deleteAllCountries()

    // Новый список стран целиком заменяет старый. Раньше список только дополнялся (REPLACE по коду страны),
    // и страны, сохранённые когда-то с координатами (0, 0), оставались в базе навсегда.
    // @Transaction: подписчики Flow не увидят пустую таблицу между удалением и вставкой
    @Transaction
    suspend fun replaceCountryList(countryLocalList: List<CountryLocal>) {
        deleteAllCountries()
        saveCountryList(countryLocalList)
    }

    @Query("SELECT * from CountryLocal")
    fun getCountryList(): Flow<List<CountryLocal>>
}