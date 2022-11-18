package com.myproject.radiojourney.presentation.content.radioStationList.adapter

import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.AsyncListDiffer
import com.myproject.radiojourney.R

class ListRadioStationAdapter:
    BaseRadioStationAdapter(R.layout.layout_radio_station_list_item) {

    // Определяем абстрактную переменную
    override val differ = AsyncListDiffer(this, diffCallback)

    // Описываем метод, который индивидуален для каждого списка - как выглядит элемент списка
    override fun onBindViewHolder(holder: RadioStationViewHolder, position: Int) {
        val radioStation = radioStationList[position]

        holder.itemView.apply {
            val textRadioStationName: AppCompatTextView =
                this.findViewById(R.id.text_radioStationName)
            val textRadioStationClickCount: AppCompatTextView =
                this.findViewById(R.id.text_radioStationClickCount2)

            textRadioStationName.text = radioStation.stationName
            textRadioStationClickCount.text = radioStation.clickCount.toString()

            setOnClickListener {
                onItemClickListener?.let { clickLambda ->
                    clickLambda(radioStation)
                }
            }
        }
    }
}