package com.myproject.radiojourney.presentation.content.radioStationList.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation

/**
 * Общий класс для списков радиостанций (вертикальных, горизонтальных и проч.)
 */
abstract class BaseRadioStationAdapter(
    private val layoutId: Int
) : RecyclerView.Adapter<BaseRadioStationAdapter.RadioStationViewHolder>() {

    // 1. DiffUtil
    protected val diffCallback = object : DiffUtil.ItemCallback<RadioStationPresentation>() {
        // if Songs have the same media id
        override fun areItemsTheSame(
            oldItem: RadioStationPresentation,
            newItem: RadioStationPresentation
        ): Boolean {
            return oldItem.urlResolved == newItem.urlResolved // In our case mediaId = url
        }

        // if songs are really the same - the image, the title and so on
        override fun areContentsTheSame(
            oldItem: RadioStationPresentation,
            newItem: RadioStationPresentation
        ): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    // private val differ = AsyncListDiffer(this, diffCallback)
    // ^ It can be created before our adapter is created, if we write it here. So, we will have reference to "this" before adapter is constructed.
    // This can lead to some problems, especially if we will use multithreading in our app. So, it's better to write:
    protected abstract val differ: AsyncListDiffer<RadioStationPresentation>

    // 2. var for setting our list of songs
    var radioStationList: List<RadioStationPresentation>
        get() = differ.currentList
        set(value) = differ.submitList(value)

    // 3. lambda for clicking on our list elements
    protected var onItemClickListener: ((RadioStationPresentation) -> Unit)? = null

    fun setItemClickListener(listener: (RadioStationPresentation) -> Unit) {
        onItemClickListener = listener
    }

    // 4. Overridden methods
    // Создаём элемент списка. Initialize itemView for each item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RadioStationViewHolder {
        return RadioStationViewHolder(
            LayoutInflater.from(parent.context).inflate(
                layoutId, // "layoutId" instead of "R.layout.list_item"
                parent,
                false
            )
        )
    }

    // onBindViewHolder - в каждом адаптере определим свою функцию (наполнение элемента списка, в каждом recycler view может быть своя прорисовка элемента списка)

    override fun getItemCount(): Int {
        return radioStationList.size
    }

    // 5. ViewHolder class
    class RadioStationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {}
}