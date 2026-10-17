package com.myproject.radiojourney.utils.exoplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.charset.Charset

/**
 * Проверка cleanStreamTitle (NowPlaying.kt): что показываем как название песни, а что считаем "названия нет".
 *
 * Это обычный unit-тест: он запускается на компьютере, без телефона и эмулятора, за секунды.
 * Так можно проверять код, который не зависит от Android, - например, разбор строк.
 * Запуск: правый клик по файлу в Android Studio -> Run, или в терминале .\gradlew testDebugUnitTest
 */
class CleanStreamTitleTest {

    private val station = "#1 HITS 80s"

    @Test
    fun normalTitle_isShownAsIs() {
        assertEquals("Kajagoogoo - Too Shy", cleanStreamTitle("Kajagoogoo - Too Shy", station))
    }

    @Test
    fun extraSpaces_areRemoved() {
        assertEquals(
            "Queen - Bohemian Rhapsody",
            cleanStreamTitle("  Queen  -   Bohemian Rhapsody ", station)
        )
    }

    @Test
    fun emptyOrOnlyPunctuation_meansNoTitle() {
        assertNull(cleanStreamTitle(null, station))
        assertNull(cleanStreamTitle("", station))
        assertNull(cleanStreamTitle(" - ", station))
        assertNull(cleanStreamTitle("...", station))
    }

    @Test
    fun stationName_isNotASong() {
        assertNull(cleanStreamTitle("#1 hits 80s", station))
    }

    // Реальная метка, которую станция прислала во время рекламы при проверке на эмуляторе
    @Test
    fun adsWizzMarker_meansNoTitle() {
        assertNull(
            cleanStreamTitle(
                "ADWTAG_122000 - THIS STATION WILL CONTINUE AFTER THIS BREAK",
                station
            )
        )
        assertNull(cleanStreamTitle("Commercial break", station))
    }

    @Test
    fun iHeartFormat_isUnwrapped() {
        assertEquals(
            "Dua Lipa - Levitating",
            cleanStreamTitle(
                "Dua Lipa - text=\"Levitating\" song_spot=\"M\" MediaBaseId=\"123\"",
                station
            )
        )
    }

    // Станция прислала "Кино - Группа крови" в windows-1251, а плеер прочитал байты как латиницу
    @Test
    fun cyrillicInWrongEncoding_isRepaired() {
        val broken = String(
            "Кино - Группа крови".toByteArray(Charset.forName("windows-1251")),
            Charsets.ISO_8859_1
        )
        assertEquals("Кино - Группа крови", cleanStreamTitle(broken, station))
    }

    // Обычные европейские буквы (ü, é) не должны "чиниться" в кириллицу
    @Test
    fun europeanLetters_areNotTouched() {
        assertEquals(
            "Die Ärzte - Männer sind Schweine",
            cleanStreamTitle("Die Ärzte - Männer sind Schweine", station)
        )
        assertEquals(
            "Édith Piaf - La Vie en rose",
            cleanStreamTitle("Édith Piaf - La Vie en rose", station)
        )
    }
}