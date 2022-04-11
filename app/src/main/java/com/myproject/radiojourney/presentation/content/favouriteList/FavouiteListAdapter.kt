package com.myproject.radiojourney.presentation.content.favouriteList

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.myproject.radiojourney.R
import com.myproject.radiojourney.model.presentation.RadioStationFavouritePresentation

// 1.1. ОБРАБОТКА КЛИКА -> передадим в конструктор анонимную функцию (как класса Adapter, так и вложенного класса). Затем отдаём эту лямбду каждому ViewHolder
class FavouiteListAdapter(
    private val favouriteStationList: List<RadioStationFavouritePresentation>,
    private val onItemClicked: (RadioStationFavouritePresentation) -> Unit,
    private val onStarClicked: (RadioStationFavouritePresentation) -> Unit
) :
    RecyclerView.Adapter<FavouiteListAdapter.FavouiteListViewHolder>() {
    companion object {
        private const val TAG = "FavouiteListAdapter"
    }

    // Создаём элемент списка. Initialize itemView for each item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavouiteListViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_radio_station_list_favourite_item, parent, false)
        return FavouiteListViewHolder(itemView, onItemClicked, onStarClicked)
    }

    // Сюда залетает элемент списка, к-рый был создан в onCreateViewHolder() и здесь мы его наполняем
    // Однако, лучше просто вызвать метод из вложенного класа, где и осуществить непосредственно наполнение, описание clickListener и проч.
    override fun onBindViewHolder(holder: FavouiteListViewHolder, position: Int) {
        holder.setFavouriteRadioStation(favouriteStationList[position])
    }

    // Возвращает количество элементов списка
    override fun getItemCount(): Int = favouriteStationList.size

    inner class FavouiteListViewHolder(
        private val itemView: View,
        private val onItemClicked: (RadioStationFavouritePresentation) -> Unit,
        private val onStarClicked: (RadioStationFavouritePresentation) -> Unit
    ) :
        RecyclerView.ViewHolder(itemView) {

        private val textRadioStationCity: AppCompatTextView =
            itemView.findViewById(R.id.text_radioStationCity)
        private val textRadioStationName: AppCompatTextView =
            itemView.findViewById(R.id.text_radioStationName)
        private val textRadioStationClickCount: AppCompatTextView =
            itemView.findViewById(R.id.text_radioStationClickCount2)
        private val imageStar: AppCompatImageView =
            itemView.findViewById(R.id.image_star)

        private val linearStationDescription: LinearLayout =
            itemView.findViewById(R.id.linear_stationDescription)
        private val linearImageStar: LinearLayout =
            itemView.findViewById(R.id.linear_imageStar)

        // 1.2. ОБРАБОТКА КЛИКА -> Будем просто возвращать элемент, на который кликнули. Создадим переменную
        private var radioStationFavourite: RadioStationFavouritePresentation? = null

        // 1.4. ОБРАБОТКА КЛИКА -> В функции init{} д.б. view.setOnClickListener{}, который передаст информацию в фрагмент, а уже из фрагмента мы будем передавать информацию во view model
        init {
            linearStationDescription.setOnClickListener {
                // ОБРАБОТКА КЛИКА -> Передадим по клику нашу переменную, если она не null (передаём в нашу анонимную функцию)
                radioStationFavourite?.let { nonNullRadioStationPresentation ->
                    onItemClicked(nonNullRadioStationPresentation)
                }
            }
            // По клику на звезду у нас будет другая функция (добавить /удалить из избранного)
            linearImageStar.setOnClickListener {
                // ОБРАБОТКА КЛИКА -> Передадим по клику нашу переменную, если она не null (передаём в нашу анонимную функцию)
                radioStationFavourite?.let { nonNullRadioStationPresentation ->
                    onStarClicked(nonNullRadioStationPresentation)
                }
            }
        }

        fun setFavouriteRadioStation(radioStationFavourite: RadioStationFavouritePresentation) {
            // 1.3. ОБРАБОТКА КЛИКА -> В методе обработки элемента списка, инициализируем нашу переменную
            this.radioStationFavourite = radioStationFavourite

            textRadioStationCity.text = radioStationFavourite.countryCode
            textRadioStationName.text = radioStationFavourite.stationName
            textRadioStationClickCount.text = radioStationFavourite.clickCount.toString()

            val isStationSavedInFavourites = radioStationFavourite.isStationInFavourite

            if (isStationSavedInFavourites) {
                imageStar.setImageResource(R.drawable.star)
            } else {
                imageStar.setImageResource(R.drawable.star_transparent)
            }

        }

    }

}