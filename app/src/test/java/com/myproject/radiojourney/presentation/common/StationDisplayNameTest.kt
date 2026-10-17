package com.myproject.radiojourney.presentation.common

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Проверка shortStationName (StationDisplayName.kt) на настоящих названиях из каталога radio-browser
 */
class StationDisplayNameTest {

    @Test
    fun shortName_isNotChanged() {
        assertEquals("Anty Radio", shortStationName("Anty Radio"))
        assertEquals("Radio X - Rock", shortStationName("Radio X - Rock"))
        // Украшения у короткого названия не трогаем - это его "лицо"
        assertEquals("*TOP 100*", shortStationName("*TOP 100*"))
    }

    @Test
    fun keywordList_afterDoubleBar_isCutOff() {
        assertEquals(
            "TOP 100 CHARTS",
            shortStationName(">> *TOP 100 CHARTS* <<  || Pop, Rock, Dance, House, Techno, Trance, Electro, Hip-Hop, Rap, RnB")
        )
        assertEquals(
            "KARNEVAL",
            shortStationName("* KARNEVAL || Fasching, Fastnacht, Alaaf, Helau, Jeck, Jeckenparty, Karnevalsparty")
        )
    }

    @Test
    fun arrowAndDash_separators() {
        // Режем по самому сильному разделителю (||): то, что осталось, уже короткое - дальше не укорачиваем
        assertEquals(
            "CLUB CHARTS -> TOP 100",
            shortStationName("#CLUB CHARTS -> TOP 100 || IMMER AKTUELL || DJ CHARTS || HOUSE, EDM, DEEP HOUSE")
        )
        assertEquals(
            "TOP 200 DJ CHARTS",
            shortStationName("#TOP 200 DJ CHARTS - JUST MUSIC - DJ REMIX & CHARTS RADIO @ TikTok Charts, Electronic Music")
        )
    }

    // После || осталась часть длиннее 40 символов - она укорачивается дальше по следующему разделителю (" - ")
    @Test
    fun longPartAfterCut_isShortenedAgain() {
        assertEquals(
            "TOP 40 DEEJAY CHARTS",
            shortStationName("# TOP 40 DEEJAY CHARTS – CLUB CHARTS NONSTOP - DANCE & DJ MIX RADIO || Ibiza House, Sunset Lounge")
        )
    }

    @Test
    fun emptyFirstPart_takesNextOne() {
        assertEquals(
            "NEUERSCHEINUNGEN",
            shortStationName(" - NEUERSCHEINUNGEN - Radio Charts - DJ Charts - Club Charts - Top 100 - Top 40 Club - Pop")
        )
    }

    @Test
    fun longNameWithoutSeparators_isOnlyTrimmed() {
        val name = "Radio Station With A Really Long Name But No Separators At All"
        assertEquals(name, shortStationName("  $name  "))
    }
}