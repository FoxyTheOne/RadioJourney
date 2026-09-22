package com.myproject.radiojourney.data.localDatabaseRoom

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Миграции базы данных: что сделать со старой базой на телефоне пользователя, когда версия схемы выросла.
 *
 * Без миграции Room при открытии базы новой версии бросает исключение, а fallbackToDestructiveMigration()
 * просто удалил бы базу вместе с избранным пользователя
 */

// Версия 13: добавлена таблица своих станций ("Мои радиостанции"). Остальные таблицы не тронуты.
// Типы колонок и NOT NULL должны совпадать с тем, что Room ожидает по MyStationLocal,
// иначе при первом же запуске будет ошибка "Migration didn't properly handle"
val MIGRATION_12_13 = Migration(12, 13) { database: SupportSQLiteDatabase ->
    database.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `MyStationLocal` (
            `stationuuid` TEXT NOT NULL,
            `stationName` TEXT NOT NULL,
            `url_resolved` TEXT NOT NULL,
            `addedAt` INTEGER NOT NULL,
            PRIMARY KEY(`stationuuid`)
        )
        """.trimIndent()
    )
}

// Версия 14: добавлена таблица сохранённых списков станций (SavedStationLocal) - их показываем, если сервер недоступен.
// Остальные таблицы не тронуты. Колонки и ключ - как в SavedStationLocal (ключ из двух колонок: страна + станция)
val MIGRATION_13_14 = Migration(13, 14) { database: SupportSQLiteDatabase ->
    database.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `SavedStationLocal` (
            `countryCode` TEXT NOT NULL,
            `stationuuid` TEXT NOT NULL,
            `stationName` TEXT NOT NULL,
            `url_resolved` TEXT NOT NULL,
            `clickCount` INTEGER NOT NULL,
            `country` TEXT NOT NULL,
            `stationCountryCode` TEXT NOT NULL,
            `position` INTEGER NOT NULL,
            `savedAt` INTEGER NOT NULL,
            PRIMARY KEY(`countryCode`, `stationuuid`)
        )
        """.trimIndent()
    )
}