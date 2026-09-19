package com.myproject.radiojourney.presentation.content.radioStationList.adapter

import android.text.SpannableString
import android.text.Spanned
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import com.myproject.radiojourney.R

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
            val isFavouritePlaylist = radioStation.countryCode.endsWith("_FAV")
            val text =
                "${radioStation.stationName} - ${radioStation.countryCode.removeSuffix("_FAV")}"

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