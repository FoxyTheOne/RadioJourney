package com.myproject.radiojourney.presentation.content.radioStationList.adapter

import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.AsyncListDiffer
import com.myproject.radiojourney.R

/**
 * Вертикальный список радиостанций (станции страны и текущий плейлист).
 * Раньше для текущего плейлиста был отдельный adapter/old/RadioListAdapter - почти такой же, но с подсветкой играющей станции
 */
class ListRadioStationAdapter :
    BaseRadioStationAdapter(R.layout.layout_radio_station_list_item) {

    // Можно ли выбрать станцию (нельзя, пока этот плейлист не скачан в плейер)
    var isClickableRecyclerView = true

    // Определяем абстрактную переменную
    override val differ = AsyncListDiffer(this, diffCallback)

    // Сейчас играющая станция - выделяется цветом
    private var currentStationUuid: String? = null

    fun setCurrentStation(stationUuid: String?) {
        if (stationUuid == currentStationUuid) return
        val oldPosition = indexOf(currentStationUuid)
        currentStationUuid = stationUuid
        if (oldPosition != -1) notifyItemChanged(oldPosition)
        val newPosition = indexOf(stationUuid)
        if (newPosition != -1) notifyItemChanged(newPosition)
    }

    fun indexOf(stationUuid: String?): Int =
        if (stationUuid == null) -1 else radioStationList.indexOfFirst { it.stationuuid == stationUuid }

    // Описываем метод, который индивидуален для каждого списка - как выглядит элемент списка
    override fun onBindViewHolder(holder: RadioStationViewHolder, position: Int) {
        val radioStation = radioStationList[position]

        holder.itemView.apply {
            findViewById<AppCompatTextView>(R.id.text_radioStationName).text =
                radioStation.stationName
            findViewById<AppCompatTextView>(R.id.text_radioStationClickCount2).text =
                radioStation.clickCount.toString()

            setBackgroundResource(
                if (radioStation.stationuuid == currentStationUuid) R.color.white_transparent_20 else R.color.background
            )

            setOnClickListener {
                if (isClickableRecyclerView) {
                    onItemClickListener?.invoke(radioStation)
                }
            }
        }
    }
}