package com.myproject.radiojourney.presentation.content.radioStationList.adapter

import android.text.SpannableString
import android.text.Spanned
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import com.myproject.radiojourney.R
import com.myproject.radiojourney.other.Constants.FAVOURITES_COUNTRY_CODE_SUFFIX
import com.myproject.radiojourney.other.Constants.MY_STATIONS_COUNTRY_CODE

/**
 * Адаптер плеера внизу экрана: одна станция - одна "страница" ViewPager2, станции листаются свайпом.
 *
 * Общая часть с обычным списком станций вынесена в BaseRadioStationAdapter (AsyncListDiffer, клик по элементу)
 */
class SwipeRadioStationAdapter :
    BaseRadioStationAdapter(R.layout.layout_radio_station_swipe_item_new) {

    var isClickableRecyclerView = true

    // Определяем абстрактную переменную
    override val differ = AsyncListDiffer(this, diffCallback)

    // Описываем метод, который индивидуален для каждого списка - как выглядит элемент списка
    override fun onBindViewHolder(holder: RadioStationViewHolder, position: Int) {
        val radioStation = radioStationList[position]
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

}