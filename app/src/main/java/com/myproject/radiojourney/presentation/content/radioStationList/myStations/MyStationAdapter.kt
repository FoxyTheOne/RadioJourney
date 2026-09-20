package com.myproject.radiojourney.presentation.content.radioStationList.myStations

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.myproject.radiojourney.R
import com.myproject.radiojourney.presentation.model.RadioStationPresentation

/**
 * Список своих станций: название, ссылка на поток и кнопка удаления.
 *
 * Что здесь можно подсмотреть для другого проекта: AsyncListDiffer сам считает разницу между старым и новым списком
 * в фоновом потоке и анимирует только изменившиеся строки - вместо notifyDataSetChanged(), который перерисовывает всё
 */
class MyStationAdapter(
    private val onStationClicked: (RadioStationPresentation) -> Unit,
    private val onEditClicked: (RadioStationPresentation) -> Unit,
    private val onDeleteClicked: (RadioStationPresentation) -> Unit
) : RecyclerView.Adapter<MyStationAdapter.MyStationViewHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<RadioStationPresentation>() {
        // Та же самая станция (по идентификатору)
        override fun areItemsTheSame(
            oldItem: RadioStationPresentation,
            newItem: RadioStationPresentation
        ): Boolean =
            oldItem.stationuuid == newItem.stationuuid

        // Содержимое не изменилось - строку можно не перерисовывать (data class сравнивается по всем полям)
        override fun areContentsTheSame(
            oldItem: RadioStationPresentation,
            newItem: RadioStationPresentation
        ): Boolean =
            oldItem == newItem
    }

    private val differ = AsyncListDiffer(this, diffCallback)

    var myStationList: List<RadioStationPresentation>
        get() = differ.currentList
        set(value) = differ.submitList(value)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyStationViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_my_station_item, parent, false)
        return MyStationViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyStationViewHolder, position: Int) {
        holder.bind(myStationList[position])
    }

    override fun getItemCount(): Int = myStationList.size

    inner class MyStationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textName: AppCompatTextView = itemView.findViewById(R.id.text_myStationName)
        private val textUrl: AppCompatTextView = itemView.findViewById(R.id.text_myStationUrl)
        private val imageEdit: AppCompatImageView = itemView.findViewById(R.id.image_editMyStation)
        private val imageDelete: AppCompatImageView =
            itemView.findViewById(R.id.image_deleteMyStation)
        private val linearDescription: View =
            itemView.findViewById(R.id.linear_myStationDescription)

        fun bind(station: RadioStationPresentation) {
            textName.text = station.stationName
            textUrl.text = station.urlResolved

            // Клик по названию - включить станцию, по карандашу - изменить, по корзине - удалить.
            // Слушатели ставятся здесь, а не в init: ViewHolder переиспользуется для разных станций
            linearDescription.setOnClickListener { onStationClicked(station) }
            imageEdit.setOnClickListener { onEditClicked(station) }
            imageDelete.setOnClickListener { onDeleteClicked(station) }
        }
    }
}