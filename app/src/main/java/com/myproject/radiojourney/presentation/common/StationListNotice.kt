package com.myproject.radiojourney.presentation.common

import android.content.Context
import com.myproject.radiojourney.R
import java.text.DateFormat
import java.util.Date

// Что сказать пользователю, если список станций запасной (см. RadioStationList.isFallback), или null, если список обычный:
// - сохранённый: "сервер недоступен, показан список, сохранённый 21 сент. 2026 г." (DateFormat сам пишет дату так,
//   как принято в языке телефона);
// - только популярные станции: "весь список не загрузился, показаны только 10 самых популярных станций"
fun Context.fallbackStationListMessage(
    savedAt: Long?,
    isOnlyPopular: Boolean,
    stationCount: Int
): String? = when {
    savedAt != null -> getString(
        R.string.savedStationList_used,
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(savedAt))
    )

    isOnlyPopular -> getString(R.string.stationList_onlyPopular, stationCount)
    else -> null
}