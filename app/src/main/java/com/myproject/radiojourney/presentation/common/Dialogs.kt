package com.myproject.radiojourney.presentation.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.myproject.radiojourney.R

/*
 * Все окна приложения с кнопками - в этом файле. Экран не собирает окно сам, а вызывает одну из функций ниже:
 * так одинаковые окна не повторяются в каждом фрагменте, и поведение (что делает "Назад", какие кнопки)
 * меняется в одном месте. Раньше "Выйти?" и "Удалить станцию?" собирались прямо во фрагментах.
 *
 * Сообщения без кнопок ("нет интернета", "сервер недоступен" поверх экрана) показывает InfoDialog.
 *
 * Оформление (фон, шрифт, цвета кнопок) задаёт тема: materialAlertDialogTheme в themes.xml.
 * MaterialAlertDialogBuilder, а не AlertDialog.Builder, - потому что он эту тему и берёт.
 *
 * Функции - расширения Context (fun Context.xxx): внутри них this - это Context, который нужен окну.
 * Во фрагменте вызов выглядит так: requireContext().showConfirmDialog(...)
 */

// Основа всех окон: заголовок и текст. Текст бывает не у всех окон ("Выйти из приложения?" - только заголовок)
private fun Context.dialogBuilder(
    @StringRes titleId: Int,
    message: CharSequence?
): MaterialAlertDialogBuilder =
    MaterialAlertDialogBuilder(this)
        .setTitle(titleId)
        .apply { if (message != null) setMessage(message) }

/**
 * Подтверждение действия: "Выйти из приложения?", "Удалить станцию?".
 * [onConfirm] вызывается по кнопке [confirmId]; "Отмена" и касание мимо окна просто закрывают его
 */
fun Context.showConfirmDialog(
    @StringRes titleId: Int,
    message: CharSequence? = null,
    @StringRes confirmId: Int,
    onConfirm: () -> Unit
): AlertDialog =
    dialogBuilder(titleId, message)
        .setPositiveButton(confirmId) { _, _ -> onConfirm() }
        .setNegativeButton(R.string.button_cancel, null)
        .show()

/**
 * Что-то не загрузилось, и без этого дальше нельзя (например, список стран на первом экране).
 * Закрыть окно мимо кнопки нельзя (setCancelable(false)): иначе пользователь остался бы на экране,
 * где ничего не происходит. Возвращает окно, чтобы экран мог закрыть его сам, когда проблема исчезла
 */
fun Context.showRetryDialog(
    @StringRes titleId: Int,
    @StringRes textId: Int,
    onRetry: () -> Unit
): AlertDialog =
    dialogBuilder(titleId, getString(textId))
        .setCancelable(false)
        .setPositiveButton(R.string.button_retry) { _, _ -> onRetry() }
        .show()

/**
 * Объяснение, зачем приложению разрешение.
 *
 * Системное окно запроса показывает только название разрешения ("доступ к местоположению"), но не причину.
 * developer.android.com рекомендует объяснять причину своим окном до системного запроса - тогда пользователь
 * понимает, что он разрешает, и реже отказывает.
 *
 * [onContinue] вызывается, когда пользователь готов увидеть системное окно.
 * [onNotNow] - когда он отказался ("Не сейчас" или касание мимо окна). Разрешение тогда не запрашиваем:
 * повторный запрос сразу же навязчив, а после двух отказов система вообще перестаёт показывать окно запроса.
 * Но экран должен продолжить работу без разрешения - например, первый экран открывает карту
 */
fun Context.showPermissionRationale(
    @StringRes titleId: Int,
    @StringRes textId: Int,
    onNotNow: () -> Unit = {},
    onContinue: () -> Unit
) {
    dialogBuilder(titleId, getString(textId))
        .setPositiveButton(R.string.permission_button_continue) { _, _ -> onContinue() }
        .setNegativeButton(R.string.permission_button_notNow) { _, _ -> onNotNow() }
        // Касание мимо окна и кнопка "Назад" закрывают окно так же, как "Не сейчас"
        .setOnCancelListener { onNotNow() }
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
    val builder = dialogBuilder(titleId, getString(textId))
        .setPositiveButton(R.string.permission_button_ok, null)
        .setOnDismissListener { onDismiss() }

    if (isPermanentlyDenied) {
        builder.setNeutralButton(R.string.permission_button_settings) { _, _ ->
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)
                )
            )
        }
    }

    builder.show()
}