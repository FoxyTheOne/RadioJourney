package com.myproject.radiojourney.utils.musicPlayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * По клику на кнопку в уведомление, сюда прилетает intent с определенным action (string) - например, play
 * Отсюда мы высылаем эти action в фрагмент (который подписан на этот фильтр), чтобы на основе этой строки вызвать соответствующий метод (play, stop, next etc.)
 */
class NotificationActionBroadcast : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.sendBroadcast(
            Intent("TRACKS_TRACKS").putExtra("action_name", intent?.action)
        )
    }
}