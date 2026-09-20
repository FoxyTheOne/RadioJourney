package com.myproject.radiojourney.presentation.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.myproject.radiojourney.R

/**
 * Объяснение, зачем приложению разрешение.
 *
 * Системное окно запроса показывает только название разрешения ("доступ к местоположению"), но не причину.
 * developer.android.com рекомендует объяснять причину своим окном до системного запроса - тогда пользователь
 * понимает, что он разрешает, и реже отказывает.
 *
 * [onContinue] вызывается, когда пользователь готов увидеть системное окно. Если он отказался - не делаем ничего:
 * запрашивать разрешение повторно сразу же нельзя (это навязчиво, а после двух отказов система вообще
 * перестаёт показывать окно запроса)
 */
fun Context.showPermissionRationale(
    @StringRes titleId: Int,
    @StringRes textId: Int,
    onContinue: () -> Unit
) {
    AlertDialog.Builder(this)
        .setTitle(titleId)
        .setMessage(textId)
        .setPositiveButton(R.string.permission_button_continue) { _, _ -> onContinue() }
        .setNegativeButton(R.string.permission_button_notNow, null)
        .show()
}

/**
 * Разрешение окончательно запрещено (пользователь отказал и попросил больше не спрашивать):
 * системное окно запроса больше не появится, изменить это можно только в настройках приложения.
 * Поэтому объясняем, что перестало работать, и предлагаем открыть настройки
 */
fun Context.showPermissionDeniedDialog(
    @StringRes titleId: Int,
    @StringRes textId: Int,
    isPermanentlyDenied: Boolean,
    onDismiss: () -> Unit = {}
) {
    val builder = AlertDialog.Builder(this)
        .setTitle(titleId)
        .setMessage(textId)
        .setPositiveButton(R.string.permission_button_ok, null)
        .setOnDismissListener { onDismiss() }

    if (isPermanentlyDenied) {
        builder.setNeutralButton(R.string.permission_button_settings) { _, _ ->
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            )
        }
    }

    builder.show()
}