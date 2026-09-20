package com.myproject.radiojourney.utils.extension

import com.myproject.radiojourney.presentation.model.RadioStationPresentation

// С какой станции начинать новый плейлист. Список отсортирован по названию, и первые станции во многих странах одинаковые
// (".Quran", "# TOP 100 ..." и т.п.), поэтому начинаем с самой популярной (больше всего прослушиваний).
// Для списка избранного оставляем первую станцию
fun List<RadioStationPresentation>.startStationIndex(): Int {
    if (isEmpty() || first().countryCode.endsWith("_FAV", true)) return 0
    return indices.maxByOrNull { this[it].clickCount } ?: 0
}