package com.myproject.radiojourney.utils.exoplayer

import androidx.annotation.OptIn
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import java.nio.charset.Charset

/**
 * Название песни, которое радиостанция передаёт прямо в потоке ("что сейчас играет").
 *
 * Откуда оно берётся:
 * - ICY-метаданные (Shoutcast, Icecast - так вещает большинство станций каталога): раз в несколько секунд в поток вставляется
 *   строка StreamTitle='Исполнитель - Песня'. ExoPlayer сам просит станцию их присылать и разбирает их в IcyInfo;
 * - ID3-теги в потоках HLS (.m3u8): TPE1 - исполнитель, TIT2 - название.
 *
 * Не все станции присылают название, а некоторые присылают своё имя, слоган или пустую строку во время рекламы.
 * Поэтому всё, что похоже на мусор, считаем "названия нет" (null): тогда в плеере просто нет строки с песней.
 * Проверить, правильное ли название прислала станция, приложение не может - другого источника нет
 */

// Название песни из пачки метаданных потока или null, если в ней нет названия
@OptIn(UnstableApi::class) // IcyInfo, TextInformationFrame
fun Metadata.streamTitle(): String? {
    var artist: String? = null
    var song: String? = null
    for (i in 0 until length()) {
        when (val entry = get(i)) {
            // ICY: одна строка, обычно "Исполнитель - Песня"
            is IcyInfo -> return entry.title
            is TextInformationFrame -> when (entry.id) {
                "TPE1" -> artist = entry.values.firstOrNull()
                "TIT2" -> song = entry.values.firstOrNull()
            }
        }
    }
    return listOfNotNull(artist?.takeIf { it.isNotBlank() }, song?.takeIf { it.isNotBlank() })
        .joinToString(" - ")
        .ifEmpty { null }
}

/**
 * Приводим название к виду, который можно показать, или возвращаем null, если показывать нечего:
 * - пустая строка, одни пробелы и знаки ("-", " - ", "..."): так станции часто "очищают" название во время рекламы;
 * - название совпадает с названием станции: это не песня;
 * - русские буквы, пришедшие "кракозябрами" (см. fixCyrillicEncoding), перекодируем обратно
 */
fun cleanStreamTitle(rawTitle: String?, stationName: String): String? {
    val title = unwrapIHeartTitle(fixCyrillicEncoding(rawTitle?.trim().orEmpty()))
        .replace(Regex("\\s+"), " ")
        .trim()
    if (title.none { it.isLetterOrDigit() }) return null
    if (title.equals(stationName.trim(), ignoreCase = true)) return null
    if (AD_MARKER.containsMatchIn(title)) return null
    return title
}

// Служебные метки рекламных систем, которые станции присылают вместо названия во время рекламы.
// Например, AdsWizz: "ADWTAG_122000 - THIS STATION WILL CONTINUE AFTER THIS BREAK" - увидели на #1 HITS 80s при проверке
private val AD_MARKER = Regex(
    "(?i)\\bADWTAG_|\\b(ad|commercial|spot)[ _-]?break\\b|will continue after (this|the) break"
)

// Станции iHeart присылают название так: Исполнитель - text="Песня" song_spot="M" MediaBaseId="..." ...
// Берём исполнителя и то, что в кавычках после text=. Остальные строки возвращаем как есть
private fun unwrapIHeartTitle(title: String): String {
    val song = Regex("text=\"([^\"]*)\"").find(title)?.groupValues?.get(1) ?: return title
    val artist = title.substringBefore("text=\"").trim().removeSuffix("-").trim()
    return listOf(artist, song.trim()).filter { it.isNotEmpty() }.joinToString(" - ")
}

/**
 * Некоторые станции присылают русское название в старой кодировке windows-1251, а ExoPlayer, не сумев прочитать
 * его как UTF-8, читает его как ISO-8859-1 (латиница). Тогда "Кино" превращается в "Êèíî".
 * Признак такой поломки: почти все буквы - из диапазона À..ÿ (U+00C0..U+00FF), куда как раз попадают
 * русские буквы windows-1251. В обычных европейских названиях таких букв мало (одна-две "é" или "ü" на слово),
 * поэтому их не трогаем. Если поломка похожа на правду - берём те же байты и читаем их как windows-1251
 */
private fun fixCyrillicEncoding(text: String): String {
    val letters = text.filter { it.isLetter() }
    if (letters.isEmpty() || text.any { it.code > 0xFF }) return text
    val latin1Letters = letters.count { it.code in 0xC0..0xFF }
    if (latin1Letters < 3 || latin1Letters * 2 < letters.length) return text
    return String(text.toByteArray(Charsets.ISO_8859_1), Charset.forName("windows-1251"))
}