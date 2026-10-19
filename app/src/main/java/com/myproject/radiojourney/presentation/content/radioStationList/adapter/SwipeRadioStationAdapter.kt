package com.myproject.radiojourney.presentation.content.radioStationList.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.myproject.radiojourney.R
import com.myproject.radiojourney.other.Constants.FAVOURITES_COUNTRY_CODE_SUFFIX
import com.myproject.radiojourney.other.Constants.MY_STATIONS_COUNTRY_CODE
import com.myproject.radiojourney.presentation.common.marqueeDurationMs
import com.myproject.radiojourney.presentation.common.shortStationName
import com.myproject.radiojourney.presentation.common.showInOneLine
import com.myproject.radiojourney.presentation.model.RadioStationPresentation

/**
 * Адаптер плеера внизу экрана: одна станция - одна "страница" ViewPager2, станции листаются свайпом.
 * На странице название станции и, если станция его присылает, название песни (setNowPlaying).
 *
 * Общая часть с обычным списком станций вынесена в BaseRadioStationAdapter (AsyncListDiffer, клик по элементу)
 */
class SwipeRadioStationAdapter :
    BaseRadioStationAdapter(R.layout.layout_radio_station_swipe_item_new) {

    companion object {
        // Пометка для notifyItemChanged: поменялось только название песни - страницу целиком не перерисовываем
        private const val PAYLOAD_NOW_PLAYING = "nowPlaying"
        private const val SLOT_ANIMATION_MS = 250L
        private const val TEXT_FADE_MS = 200L

        // Станция замолчала (реклама, джингл, промежуток между песнями): текст гаснет сразу, а место под строкой
        // закрывается через столько миллисекунд. Многие станции присылают пустое название на секунду-другую
        // между двумя песнями - без этой паузы название станции съезжало бы вниз и тут же возвращалось наверх
        private const val SLOT_CLOSE_DELAY_MS = 3000L
    }

    var isClickableRecyclerView = true

    // --- Название песни (вторая строка страницы) ---
    // Станция, у которой открыто место под название песни ("uuid|PL", как MusicServiceConnection.NowPlaying.stationKey).
    // Место открыто, только пока есть что написать: название станции поднимается наверх, когда пришло название песни,
    // и возвращается в центр, когда станция перестала его присылать (через SLOT_CLOSE_DELAY_MS) или в плеере другая станция
    private var nowPlayingSlotKey: String? = null
    private var nowPlayingTitle: String? = null

    // Отложенное закрытие места (см. SLOT_CLOSE_DELAY_MS). Handler главного потока выполнит closeSlot через заданное время,
    // removeCallbacks отменяет это, если раньше пришло новое название
    private val mainHandler = Handler(Looper.getMainLooper())
    private val closeSlot = Runnable { moveSlotTo(null) }

    // Определяем абстрактную переменную
    override val differ = AsyncListDiffer(this, diffCallback)

    /**
     * Что сейчас играет: stationKey - станция в плеере (null - станции нет), title - название песни
     * или null, если станция его сейчас не сообщает. Старое название при этом не остаётся: строка гаснет сразу
     */
    fun setNowPlaying(stationKey: String?, title: String?) {
        mainHandler.removeCallbacks(closeSlot)
        nowPlayingTitle = title
        when {
            // Есть что написать - место открыто у этой станции
            title != null -> moveSlotTo(stationKey)
            // Та же станция замолчала: текст гаснет сразу, а место закроется чуть позже
            stationKey != null && stationKey == nowPlayingSlotKey -> {
                notifyStationChanged(stationKey)
                mainHandler.postDelayed(closeSlot, SLOT_CLOSE_DELAY_MS)
            }
            // Другая станция, и она пока молчит - места нет, название станции по центру
            else -> moveSlotTo(null)
        }
    }

    override fun onBindViewHolder(
        holder: RadioStationViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (PAYLOAD_NOW_PLAYING in payloads) {
            bindNowPlaying(holder.itemView, radioStationList[position], animate = true)
        } else {
            super.onBindViewHolder(
                holder,
                position,
                payloads
            ) // вызовет обычный onBindViewHolder ниже
        }
    }

    // Описываем метод, который индивидуален для каждого списка - как выглядит элемент списка
    override fun onBindViewHolder(holder: RadioStationViewHolder, position: Int) {
        val radioStation = radioStationList[position]
        holder.itemView.apply {
            val title: AppCompatTextView = this.findViewById(R.id.tvPrimary)

            // У станций из плейлиста избранного код страны с суффиксом "_FAV" (например "PL_FAV").
            // Показываем вместо суффикса сердечко перед названием: "♥ RMF FM - PL"
            val isFavouritePlaylist =
                radioStation.countryCode.endsWith(FAVOURITES_COUNTRY_CODE_SUFFIX)
            // Короткое имя: у длинных названий отрезаем слоган и список жанров (см. shortStationName).
            // У своих станций страны нет, поэтому показываем одно название: код "MY" пользователю ничего не скажет
            val name = shortStationName(radioStation.stationName)
            val text = if (radioStation.countryCode == MY_STATIONS_COUNTRY_CODE) {
                name
            } else {
                "$name - ${radioStation.countryCode.removeSuffix(FAVOURITES_COUNTRY_CODE_SUFFIX)}"
            }

            if (isFavouritePlaylist) {
                // Сердечко - векторная картинка, а не символ "♥": символ в разных шрифтах и на разных телефонах
                // может выглядеть по-разному (или стать цветным эмодзи), а картинка отображается везде одинаково
                val spannableText = SpannableString("  $text") // первый пробел заменяется картинкой
                ContextCompat.getDrawable(context, R.drawable.ic_baseline_favorite_24_orange)
                    ?.let { heart ->
                        val size = (title.textSize * 0.9f).toInt()
                        heart.setBounds(0, 0, size, size)
                        spannableText.setSpan(
                            ImageSpan(heart, DynamicDrawableSpan.ALIGN_BASELINE),
                            0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                title.showInOneLine(spannableText)
            } else {
                title.showInOneLine(text)
            }

            // Строка песни - после названия станции: ей нужно знать, поедет ли станция (см. bindNowPlaying)
            bindNowPlaying(this, radioStation, animate = false)

            setOnClickListener {

                if (isClickableRecyclerView) {
                    onItemClickListener?.let { clickLambda ->
                        clickLambda(radioStation)
                    }
                }

            }
            // Долгое нажатие - скопировать название песни. На всю страницу, а не только на строку песни:
            // строка узкая, в неё трудно попасть, и обычное нажатие на ней перестало бы включать радио
            setOnLongClickListener { copyNowPlaying(this, radioStation) }
        }
    }

    // Та же запись, что у станции в плеере: одна станция бывает и в списке страны, и в избранном ("PL" и "PL_FAV")
    private fun RadioStationPresentation.stationKey() = "$stationuuid|$countryCode"

    /**
     * Вторая строка страницы. animate = false - страница рисуется заново (пролистали, новый плейлист): сразу как надо.
     * animate = true - пришло новое название у станции, которую видно сейчас:
     * - место открывается или закрывается: название станции плавно отъезжает (ChangeBounds), строка песни проявляется;
     * - место уже открыто: на месте меняется только текст - старый гаснет, новый проявляется, ничего не двигается
     */
    private fun bindNowPlaying(page: View, station: RadioStationPresentation, animate: Boolean) {
        val stationText: TextView = page.findViewById(R.id.tvPrimary)
        val songText: TextView = page.findViewById(R.id.tvNowPlaying)
        val hasSlot = station.stationKey() == nowPlayingSlotKey
        val title = if (hasSlot) nowPlayingTitle.orEmpty() else ""

        songText.animate().cancel()
        if (!animate) {
            // Страница рисуется вместе с названием станции: если оно тоже не помещается и поедет, строка песни
            // подождёт, пока станция доедет (marqueeDurationMs). Ширину станции узнаём, когда она измерена (doOnLayout)
            songText.isVisible = hasSlot
            songText.alpha = 1f
            if (hasSlot) stationText.doOnLayout {
                songText.showInOneLine(
                    title,
                    startDelayMs = stationText.marqueeDurationMs()
                )
            }
            return
        }

        if (hasSlot != songText.isVisible) {
            TransitionManager.beginDelayedTransition(
                page as ViewGroup,
                ChangeBounds().setDuration(SLOT_ANIMATION_MS)
            )
            songText.isVisible = hasSlot
            songText.showInOneLine(title)
            songText.alpha = 0f
            if (hasSlot) songText.animate().alpha(1f).setStartDelay(SLOT_ANIMATION_MS / 2)
                .setDuration(SLOT_ANIMATION_MS).start()
            return
        }

        if (!hasSlot || songText.text.toString() == title) return
        songText.animate().alpha(0f).setStartDelay(0).setDuration(TEXT_FADE_MS).withEndAction {
            songText.showInOneLine(title) // новая песня - бегущая строка (если нужна) поедет заново
            // Пустое название (станция замолчала) - строка остаётся погасшей, пока место не закроется (SLOT_CLOSE_DELAY_MS)
            if (title.isNotEmpty()) songText.animate().alpha(1f).setDuration(TEXT_FADE_MS).start()
        }.start()
    }

    // Открыть место под песню у станции newSlotKey (null - ни у какой) и перерисовать затронутые страницы
    private fun moveSlotTo(newSlotKey: String?) {
        val oldSlotKey = nowPlayingSlotKey
        nowPlayingSlotKey = newSlotKey
        setOfNotNull(oldSlotKey, newSlotKey).forEach(::notifyStationChanged)
    }

    // Перерисовываем только страницу этой станции и с пометкой PAYLOAD_NOW_PLAYING: onBindViewHolder с payloads
    // меняет строку плавно, а не перерисовывает страницу целиком
    private fun notifyStationChanged(stationKey: String) {
        val position = radioStationList.indexOfFirst { it.stationKey() == stationKey }
        if (position != -1) notifyItemChanged(position, PAYLOAD_NOW_PLAYING)
    }

    /**
     * Долгое нажатие на страницу - скопировать название песни (например, чтобы потом найти песню).
     * true - скопировали: для долгого нажатия это значит "обработано", телефон слегка вибрирует, обычный клик не срабатывает.
     * false - песни сейчас нет: долгое нажатие ничего не копирует
     */
    private fun copyNowPlaying(page: View, station: RadioStationPresentation): Boolean {
        val title = nowPlayingTitle
        if (station.stationKey() != nowPlayingSlotKey || title.isNullOrEmpty()) return false

        val context = page.context
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                context.getString(R.string.player_songTitle),
                title
            )
        )
        // С Android 13 система сама показывает внизу экрана, что скопировано - наша подсказка была бы второй такой же
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, R.string.player_songCopied, Toast.LENGTH_SHORT).show()
        }
        return true
    }

}