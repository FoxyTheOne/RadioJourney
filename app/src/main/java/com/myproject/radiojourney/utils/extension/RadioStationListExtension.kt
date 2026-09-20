package com.myproject.radiojourney.utils.extension

import com.myproject.radiojourney.presentation.model.RadioStationPresentation

/**
 * Расширения для списка станций.
 *
 * startStationIndex() - с какой станции начинать новый плейлист. Не с первой по алфавиту: в большинстве стран
 * это одинаковые "служебные" станции вроде ".Quran" или "# TOP 100", поэтому берётся самая популярная
 */
// С какой станции начинать новый плейлист. Список отсортирован по названию, и первые станции во многих странах одинаковые
// (".Quran", "# TOP 100 ..." и т.п.), поэтому начинаем с самой популярной (больше всего прослушиваний).
// Для списка избранного оставляем первую станцию
fun List<RadioStationPresentation>.startStationIndex(): Int {
    if (isEmpty() || first().countryCode.endsWith("_FAV", true)) return 0
    return indices.maxByOrNull { this[it].clickCount } ?: 0
}