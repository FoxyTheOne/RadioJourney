package com.myproject.radiojourney.presentation.content.radioStationList.adapter

import android.text.SpannableString
import android.text.Spanned
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.myproject.radiojourney.R
import com.myproject.radiojourney.other.Constants.FAVOURITES_COUNTRY_CODE_SUFFIX
import com.myproject.radiojourney.other.Constants.MY_STATIONS_COUNTRY_CODE
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
    }

    var isClickableRecyclerView = true

    // --- Название песни (вторая строка страницы) ---
    // Станция, у которой открыто место под название песни ("uuid|PL", как MusicServiceConnection.NowPlaying.stationKey).
    // Место открывается, когда станция впервые прислала название, и закрывается, только когда в плеере другая станция.
    // Пока оно открыто, название станции стоит наверху и не прыгает, даже если песни сейчас нет (реклама)
    private var nowPlayingSlotKey: String? = null
    private var nowPlayingTitle: String? = null

    /**
     * Что сейчас играет: stationKey - станция в плеере (null - станции нет), title - название песни
     * или null, если станция его сейчас не сообщает. Старое название при этом не остаётся: строка гаснет и ждёт новое
     */
    fun setNowPlaying(stationKey: String?, title: String?) {
        val oldSlotKey = nowPlayingSlotKey
        nowPlayingSlotKey = when {
            stationKey == null -> null
            stationKey == oldSlotKey -> oldSlotKey // та же станция: место остаётся, даже если названия сейчас нет
            title != null -> stationKey // станция впервые прислала название - открываем место
            else -> null // другая станция, и она пока молчит - места нет, название станции по центру
        }
        nowPlayingTitle = title
        // Перерисовываем только затронутые страницы и с пометкой PAYLOAD_NOW_PLAYING: onBindViewHolder с payloads
        // меняет строку плавно, а не перерисовывает страницу целиком
        setOfNotNull(oldSlotKey, nowPlayingSlotKey).forEach { key ->
            val position = radioStationList.indexOfFirst { it.stationKey() == key }
            if (position != -1) notifyItemChanged(position, PAYLOAD_NOW_PLAYING)
        }
    }

    // Определяем абстрактную переменную
    override val differ = AsyncListDiffer(this, diffCallback)

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
        bindNowPlaying(holder.itemView, radioStation, animate = false)
        holder.itemView.apply {
            val title: AppCompatTextView = this.findViewById(R.id.tvPrimary)

            // У станций из плейлиста избранного код страны с суффиксом "_FAV" (например "PL_FAV").
            // Показываем вместо суффикса сердечко перед названием: "♥ RMF FM - PL"
            val isFavouritePlaylist =
                radioStation.countryCode.endsWith(FAVOURITES_COUNTRY_CODE_SUFFIX)
            // У своих станций страны нет, поэтому показываем одно название: код "MY" пользователю ничего не скажет
            val text = if (radioStation.countryCode == MY_STATIONS_COUNTRY_CODE) {
                radioStation.stationName
            } else {
                "${radioStation.stationName} - ${
                    radioStation.countryCode.removeSuffix(
                        FAVOURITES_COUNTRY_CODE_SUFFIX
                    )
                }"
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
                title.text = spannableText
            } else {
                title.text = text
            }

            setOnClickListener {

                if (isClickableRecyclerView) {
                    onItemClickListener?.let { clickLambda ->
                        clickLambda(radioStation)
                    }
                }

            }
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
            stationText.maxLines =
                if (hasSlot) 1 else 2 // две строки станции и строка песни не поместятся в 56 dp
            songText.isVisible = hasSlot
            songText.text = title
            songText.alpha = 1f
            return
        }

        if (hasSlot != songText.isVisible) {
            TransitionManager.beginDelayedTransition(
                page as ViewGroup,
                ChangeBounds().setDuration(SLOT_ANIMATION_MS)
            )
            stationText.maxLines = if (hasSlot) 1 else 2
            songText.isVisible = hasSlot
            songText.text = title
            songText.alpha = 0f
            if (hasSlot) songText.animate().alpha(1f).setStartDelay(SLOT_ANIMATION_MS / 2)
                .setDuration(SLOT_ANIMATION_MS).start()
            return
        }

        if (!hasSlot || songText.text.toString() == title) return
        songText.animate().alpha(0f).setStartDelay(0).setDuration(TEXT_FADE_MS).withEndAction {
            songText.text = title
            // Пустое название (станция замолчала) - строка так и остаётся погасшей до следующей песни
            if (title.isNotEmpty()) songText.animate().alpha(1f).setDuration(TEXT_FADE_MS).start()
        }.start()
    }

}