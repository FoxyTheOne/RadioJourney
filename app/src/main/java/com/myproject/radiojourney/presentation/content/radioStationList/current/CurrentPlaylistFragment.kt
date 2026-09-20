package com.myproject.radiojourney.presentation.content.radioStationList.current

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.myproject.radiojourney.R
import com.myproject.radiojourney.presentation.MainViewModel
import com.myproject.radiojourney.presentation.common.InfoDialog
import com.myproject.radiojourney.presentation.common.collectWhenStarted
import com.myproject.radiojourney.presentation.content.radioStationList.adapter.ListRadioStationAdapter
import com.myproject.radiojourney.presentation.content.radioStationList.base.BaseRadioListFragmentAbstract
import dagger.hilt.android.AndroidEntryPoint

/**
 * Страница со списком радиостанций в текущем плейлисте
 */
@AndroidEntryPoint
class CurrentPlaylistFragment : BaseRadioListFragmentAbstract() {

    // ViewModel плейера привязана к Activity, а не к фрагменту
    private val mainViewModel by activityViewModels<MainViewModel>()

    private lateinit var infoDialog: InfoDialog
    private lateinit var recyclerViewRadioStationList: RecyclerView
    private val radioListAdapter = ListRadioStationAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<AppCompatTextView>(R.id.text_myFavorites_title).text = getString(R.string.currentPlaylist_title)
        view.findViewById<AppCompatTextView>(R.id.text_radioStationDialogTitleSelect).isVisible = false
        view.findViewById<AppCompatTextView>(R.id.text_radioStationDialogTitleDownload).isVisible = false
        recyclerViewRadioStationList = view.findViewById(R.id.recyclerView_radioStationList)

        infoDialog = InfoDialog(requireContext())
        infoDialog.showIfNoInternet()

        // Список станций текущего плейлиста - тот же, что в плейере
        val radioStationPlaylist = mainViewModel.currentPlaylistStations
        if (radioStationPlaylist.isNotEmpty()) {
            val currentStationUuid = mainViewModel.curPlayingSong.value?.mediaId
            radioListAdapter.setItemClickListener { radioStation ->
                // Открываем по клику другой фрагмент, передаём туда нашу радиостанцию
                if (findNavController().currentDestination?.id == R.id.currentPlaylistFragment) {
                    findNavController().navigate(
                        CurrentPlaylistFragmentDirections.actionCurrentPlaylistFragmentToHomeRadioFragment(radioStation)
                    )
                }
            }
            recyclerViewRadioStationList.adapter = radioListAdapter
            radioListAdapter.submitRadioStationList(radioStationPlaylist) {
                // Список открывается на играющей станции (она подсвечена)
                radioListAdapter.setCurrentStation(currentStationUuid)
                scrollToStation(radioListAdapter.indexOf(currentStationUuid))
            }

            // Станция переключилась, пока открыт список - переносим подсветку
            viewLifecycleOwner.collectWhenStarted(mainViewModel.curPlayingSong) { mediaItem ->
                radioListAdapter.setCurrentStation(mediaItem?.mediaId)
            }
        } else {
            view.findViewById<TextView>(R.id.text_favouritesEmpty).apply {
                text = getString(R.string.currentPlaylist_empty)
                isVisible = true
            }
        }

        view.findViewById<AppCompatImageView>(R.id.image_arrowBack).setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.currentPlaylistFragment) {
                findNavController().navigate(R.id.action_currentPlaylistFragment_to_homeRadioFragment)
            }
        }
    }

    // Прокручиваем список так, чтобы играющая станция была примерно на трети высоты экрана
    private fun scrollToStation(position: Int) {
        if (position < 0) return
        recyclerViewRadioStationList.doOnLayout {
            recyclerViewRadioStationList.post {
                (recyclerViewRadioStationList.layoutManager as? LinearLayoutManager)
                    ?.scrollToPositionWithOffset(position, recyclerViewRadioStationList.height / 3)
            }
        }
    }

    override fun onDestroyView() {
        infoDialog.dismiss() // Открытый диалог закрываем вместе с экраном, иначе WindowLeaked
        super.onDestroyView()
    }
}