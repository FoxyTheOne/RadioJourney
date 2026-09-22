package com.myproject.radiojourney.presentation.common

import android.app.Dialog
import android.content.Context
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatTextView
import com.myproject.radiojourney.R
import com.myproject.radiojourney.other.ServerError

/**
 * Диалог с заголовком и текстом ("нет интернета", "сервер недоступен" и т.п.).
 * Раньше создание Dialog и одинаковая функция showCustomDialog() были скопированы в каждый экран
 *
 * Диалог нужно закрыть (dismiss) вместе с экраном, иначе WindowLeaked
 */
class InfoDialog(
    private val context: Context,
    @LayoutRes layoutId: Int = R.layout.layout_internet_trouble_dialog,
    @IdRes private val titleViewId: Int = R.id.title_internetTrouble,
    @IdRes private val textViewId: Int = R.id.text_internetTrouble
) {
    // Тема окна - общая для всех диалогов приложения (см. themes.xml): фон, скругление и цвета текста
    private val dialog = Dialog(context, R.style.ThemeOverlay_RadioJourney_CustomDialog).apply {
        setContentView(layoutId)
    }

    fun show(@StringRes titleId: Int, @StringRes textId: Int) {
        dialog.findViewById<AppCompatTextView>(titleViewId).text = context.getString(titleId)
        dialog.findViewById<AppCompatTextView>(textViewId).text = context.getString(textId)
        dialog.show()
    }

    // Не удалось получить данные с сервера radio-browser. Раньше при любой неудаче было одно окно
    // "В ответ на запрос получен пустой список" - теперь текст объясняет, что именно случилось
    fun showServerError(reason: ServerError) = when (reason) {
        ServerError.NO_NETWORK -> show(
            R.string.serverError_noNetwork_title,
            R.string.serverError_noNetwork_text
        )

        ServerError.SERVER_NOT_RESPONDING -> show(
            R.string.serverError_notResponding_title,
            R.string.serverError_notResponding_text
        )

        ServerError.CONNECTION_CUT -> show(
            R.string.serverError_connectionCut_title,
            R.string.serverError_connectionCut_text
        )
    }

    // Показать диалог "нет интернета", если подключения нет
    fun showIfNoInternet() {
        if (!context.isInternetAvailable()) {
            show(R.string.dialogInternetTrouble_title, R.string.dialogInternetTrouble_text3)
        }
    }

    fun hide() = dialog.hide()

    fun dismiss() = dialog.dismiss()
}
