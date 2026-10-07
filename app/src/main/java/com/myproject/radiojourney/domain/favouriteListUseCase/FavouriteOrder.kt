package com.myproject.radiojourney.domain.favouriteListUseCase

import java.text.Collator
import java.util.Locale

/**
 * Порядок станций в избранном: по названию страны на языке телефона, внутри страны - по названию станции.
 *
 * Один и тот же порядок нужен в двух местах: в списке избранного (там страны идут группами с заголовком)
 * и в плейлисте избранного в плеере - иначе при перелистывании станций в плеере они шли бы не так, как в списке.
 * Раньше порядок был тот, в каком станции попали в базу, и станции одной страны оказывались вперемешку.
 *
 * Функция общая (generic, <T>): список избранного хранит модели presentation, а плеер получает модели domain.
 * Каждый передаёт, где у его модели код страны и название станции
 */
fun <T> favouriteOrder(countryCode: (T) -> String, stationName: (T) -> String): Comparator<T> {
    // Collator сравнивает строки по правилам языка телефона: строчные и заглавные буквы стоят вместе,
    // "Ё" - рядом с "Е", "Österreich" - рядом с "O". Обычное сравнение строк ставит все заглавные буквы раньше строчных
    val collator = Collator.getInstance()
    return compareBy<T, String>(collator) { countryName(countryCode(it)) }
        .thenBy(collator) { stationName(it).trim() }
}

// Название страны на языке телефона по её коду ("PL" -> "Польша"). По нему же список избранного подписывает группы
fun countryName(countryCode: String): String = Locale("", countryCode).displayCountry
