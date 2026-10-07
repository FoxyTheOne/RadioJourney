package com.myproject.radiojourney.presentation.content.radioStationList.favourite

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.myproject.radiojourney.R
import com.myproject.radiojourney.domain.favouriteListUseCase.countryName
import com.myproject.radiojourney.presentation.model.RadioStationPresentation

// 1.1. ОБРАБОТКА КЛИКА -> передадим в конструктор анонимную функцию (как класса Adapter, так и вложенного класса). Затем отдаём эту лямбду каждому ViewHolder
/**
 * Адаптер списка избранного: станции сгруппированы по странам (название страны - заголовком над группой),
 * у каждой станции - количество прослушиваний и звезда.
 *
 * Два обработчика клика (по элементу и по звезде) передаются в конструктор лямбдами - адаптер не знает,
 * что происходит дальше, и его можно использовать на любом экране
 */
class FavoriteListAdapter(
    private val onItemClicked: (RadioStationPresentation) -> Unit,
    private val onStarClicked: (RadioStationPresentation) -> Unit
) :
    RecyclerView.Adapter<FavoriteListAdapter.FavoriteListViewHolder>() {

    // Список станций. Адаптер создаётся один раз, а список обновляется: раньше при каждом изменении списка создавался новый адаптер
    var favouriteStationList: List<RadioStationPresentation> = emptyList()
        @SuppressLint("NotifyDataSetChanged") // список небольшой, а звезда меняется у элемента на месте
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    // Можно ли выбрать станцию из списка. Выбор станции включает плейлист избранного в плеере, поэтому он разрешён,
    // только когда этот плейлист уже скачан. Звезда работает всегда: добавление в избранное меняет только базу данных
    var isStationClickable = false

    // Создаём элемент списка. Initialize itemView for each item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteListViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_radio_station_list_favourite_item, parent, false)
        return FavoriteListViewHolder(itemView, onItemClicked, onStarClicked)
    }

    // Сюда залетает элемент списка, к-рый был создан в onCreateViewHolder() и здесь мы его наполняем
    // Однако, лучше просто вызвать метод из вложенного класса, где и осуществить непосредственно наполнение, описание clickListener и проч.
    override fun onBindViewHolder(holder: FavoriteListViewHolder, position: Int) {
        val station = favouriteStationList[position]
        // Список отсортирован по странам (см. favouriteOrder), станции одной страны идут подряд.
        // Название страны показываем один раз - над первой станцией страны, как заголовок группы.
        // Раньше оно повторялось над каждой станцией, и при 50 станциях список превращался в пёструю ленту
        val isFirstInCountry =
            position == 0 || favouriteStationList[position - 1].countryCode != station.countryCode
        holder.setFavouriteRadioStation(station, isFirstInCountry)
    }

    // Возвращает количество элементов списка
    override fun getItemCount(): Int = favouriteStationList.size

    inner class FavoriteListViewHolder(
        itemView: View,
        private val onItemClicked: (RadioStationPresentation) -> Unit,
        private val onStarClicked: (RadioStationPresentation) -> Unit
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
        private var radioStationFavourite: RadioStationPresentation? = null

        // 1.4. ОБРАБОТКА КЛИКА -> В функции init{} д.б. view.setOnClickListener{}, который передаст информацию в фрагмент, а уже из фрагмента мы будем передавать информацию во view model
        init {
            linearStationDescription.setOnClickListener {

                if (isStationClickable) {
                    // ОБРАБОТКА КЛИКА -> Передадим по клику нашу переменную, если она не null (передаём в нашу анонимную функцию)
                    radioStationFavourite?.let { nonNullRadioStationPresentation ->
                        onItemClicked(nonNullRadioStationPresentation)
                    }
                }

            }
            // По клику на звезду у нас будет другая функция (добавить / удалить из избранного).
            // Звезда нажимается всегда, даже если в плеере сейчас другой плейлист: раньше она была заблокирована
            // вместе со всем списком, и убрать станцию из избранного можно было, только скачав плейлист избранного
            linearImageStar.setOnClickListener {
                // ОБРАБОТКА КЛИКА -> Передадим по клику нашу переменную, если она не null (передаём в нашу анонимную функцию)
                radioStationFavourite?.let { nonNullRadioStationPresentation ->
                    onStarClicked(nonNullRadioStationPresentation)
                }
            }
        }

        fun setFavouriteRadioStation(
            radioStationFavourite: RadioStationPresentation,
            isFirstInCountry: Boolean
        ) {
            // 1.3. ОБРАБОТКА КЛИКА -> В методе обработки элемента списка, инициализируем нашу переменную
            this.radioStationFavourite = radioStationFavourite

            // Заголовок группы - название страны на языке телефона. isVisible ставим в обе стороны:
            // элементы списка переиспользуются, и без "= false" заголовок остался бы от прошлой станции
            textRadioStationCity.isVisible = isFirstInCountry
            if (isFirstInCountry) textRadioStationCity.text =
                countryName(radioStationFavourite.countryCode)

            textRadioStationName.text = radioStationFavourite.stationName
            textRadioStationClickCount.text = radioStationFavourite.clickCount.toString()

            if (radioStationFavourite.isStationInFavourite) {
                imageStar.setImageResource(R.drawable.ic_baseline_star_24_orange)
            } else {
                imageStar.setImageResource(R.drawable.ic_baseline_star_border_24_orange)
            }
        }

    }

}