package com.myproject.radiojourney.presentation.common

import android.app.Dialog
import android.content.Context
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatTextView
import com.myproject.radiojourney.R

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
    private val dialog = Dialog(context).apply { setContentView(layoutId) }

    fun show(@StringRes titleId: Int, @StringRes textId: Int) {
        dialog.findViewById<AppCompatTextView>(titleViewId).text = context.getString(titleId)
        dialog.findViewById<AppCompatTextView>(textViewId).text = context.getString(textId)
        dialog.show()
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
