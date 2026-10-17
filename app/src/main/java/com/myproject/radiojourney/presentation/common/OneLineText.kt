package com.myproject.radiojourney.presentation.common

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.widget.TextView
import com.myproject.radiojourney.R

/**
 * Текст в одну строку для нижнего плеера: если не помещается - бегущая строка (marquee), но сдержанная.
 *
 * Бегущая строка встроена в TextView (ellipsize = MARQUEE). Она едет, только пока у TextView isSelected = true,
 * начинает сама примерно через 1,2 секунды (глаз успевает прочитать начало) и проезжает marqueeRepeatLimit раз,
 * потом останавливается в начале. Короткий текст, который помещается, не двигается вовсе.
 *
 * Когда бегущей строки нет, а вместо неё многоточие в конце:
 * - текст длиннее MARQUEE_MAX_LENGTH символов: со скоростью бегущей строки (около трёх символов в секунду) он ехал бы
 *   слишком долго. Такие названия плеер сначала сокращает (shortStationName), но на всякий случай есть и этот предел;
 * - в настройках телефона выключены анимации (так делают люди, которых утомляет движение на экране) - уважаем это
 */

// Длиннее - не едет, а обрезается многоточием. Около 60 символов проезжают примерно за 10 секунд
private const val MARQUEE_MAX_LENGTH = 60

// Сколько раз строка проезжает, прежде чем остановиться. Снова поедет, когда сменится текст (станция или песня)
private const val MARQUEE_PASSES = 2

// Так работает бегущая строка в самом TextView: старт через 1,2 с, скорость 30 dp в секунду,
// после текста - промежуток в треть его длины. Нужно, чтобы посчитать, сколько едет строка (marqueeDurationMs)
private const val MARQUEE_START_DELAY_MS = 1200L
private const val MARQUEE_DP_PER_SECOND = 30f

// Ширина мягкого затухания у краев бегущей строки
private const val FADING_EDGE_DP = 16f

/**
 * Показать текст в одну строку. startDelayMs - подождать, прежде чем разрешить строке ехать:
 * так строка песни не стартует одновременно со строкой станции (две бегущие строки сразу - это уже суета)
 */
fun TextView.showInOneLine(text: CharSequence, startDelayMs: Long = 0L) {
    // Отменяем запуск, запланированный для прошлого текста
    (getTag(R.id.tag_marquee_start) as? Runnable)?.let { removeCallbacks(it) }

    setSingleLine(true)
    this.text = text
    val canMove = text.length <= MARQUEE_MAX_LENGTH && areAnimationsEnabled(context)
    ellipsize = if (canMove) TextUtils.TruncateAt.MARQUEE else TextUtils.TruncateAt.END
    marqueeRepeatLimit = MARQUEE_PASSES
    // Мягкое затухание по краям: бегущий текст не обрывается резко у края, а плавно растворяется.
    // У текста, который помещается, края пустые - затухание там ничего не меняет
    isHorizontalFadingEdgeEnabled = canMove
    setFadingEdgeLength((FADING_EDGE_DP * resources.displayMetrics.density).toInt())
    isSelected = false
    if (!canMove) return

    val start = Runnable { isSelected = true }
    setTag(R.id.tag_marquee_start, start)
    postDelayed(start, startDelayMs)
}

/**
 * Сколько миллисекунд будет ехать бегущая строка с текущим текстом (с ожиданием старта и всеми проходами).
 * 0 - текст помещается, или вместо бегущей строки многоточие. Вызывать после того, как TextView измерен (doOnLayout)
 */
fun TextView.marqueeDurationMs(): Long {
    if (ellipsize != TextUtils.TruncateAt.MARQUEE) return 0L
    val visibleWidth = width - totalPaddingLeft - totalPaddingRight
    val textWidth = paint.measureText(text, 0, text.length)
    if (visibleWidth <= 0 || textWidth <= visibleWidth) return 0L
    val pxPerSecond = MARQUEE_DP_PER_SECOND * resources.displayMetrics.density
    val onePassMs = (textWidth + textWidth / 3) / pxPerSecond * 1000
    return MARQUEE_START_DELAY_MS + (onePassMs * MARQUEE_PASSES).toLong()
}

// Анимации в телефоне включены? На Android 8+ это знает ValueAnimator, на 7 - смотрим настройку "Длительность анимации"
private fun areAnimationsEnabled(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ValueAnimator.areAnimatorsEnabled()
    } else {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) != 0f
    }