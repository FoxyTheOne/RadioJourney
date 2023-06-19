package com.myproject.radiojourney.presentation.content.radioStationList.adapter

import androidx.appcompat.widget.AppCompatTextView
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
            val text = "${radioStation.stationName} - ${radioStation.countryCode}"

            val title: AppCompatTextView = this.findViewById(R.id.tvPrimary)
            title.text = text

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