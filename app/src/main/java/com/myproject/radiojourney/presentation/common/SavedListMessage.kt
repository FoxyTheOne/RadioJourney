package com.myproject.radiojourney.presentation.common

import android.content.Context
import com.myproject.radiojourney.R
import java.text.DateFormat
import java.util.Date

// Текст "сервер недоступен, показан список, сохранённый 21 сент. 2026 г.".
// DateFormat.getDateInstance сам пишет дату так, как принято в языке телефона
fun Context.savedStationListMessage(savedAt: Long): String =
    getString(
        R.string.savedStationList_used,
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(savedAt))
    )