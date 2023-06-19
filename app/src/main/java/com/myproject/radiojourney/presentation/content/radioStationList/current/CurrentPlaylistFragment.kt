package com.myproject.radiojourney.presentation.content.radioStationList.current

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.myproject.radiojourney.R
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import com.myproject.radiojourney.presentation.MainViewModel
import com.myproject.radiojourney.presentation.content.radioStationList.adapter.old.RadioListAdapter
import com.myproject.radiojourney.presentation.content.radioStationList.base.BaseRadioListFragmentAbstract
import com.myproject.radiojourney.utils.exoplayer.MusicServiceConnection
import com.myproject.radiojourney.utils.extension.isPlayEnabled
import com.myproject.radiojourney.utils.extension.isPrepared
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Страница со списком радиостанций в текущем плейлисте
 */
@AndroidEntryPoint
class CurrentPlaylistFragment : BaseRadioListFragmentAbstract() {
    companion object {
        private const val TAG = "CurrentPlaylistFragment"
    }

    // 1.1. ViewModel. We bind our viewModel to the cycle of our activity, not fragment. So, we need to do this way:
    private lateinit var mainViewModel: MainViewModel

    @Inject
    lateinit var musicServiceConnection: MusicServiceConnection

    private var radioStationPlaylist: List<RadioStationPresentation> = emptyList()
    private lateinit var textRadioListTitle: AppCompatTextView
    private lateinit var textRadioListSecondTitleSelect: AppCompatTextView
    private lateinit var textRadioListSecondTitleDownload: AppCompatTextView
    private lateinit var imageArrowBack: AppCompatImageView
    private lateinit var radioListAdapter: RadioListAdapter
    private lateinit var recyclerViewRadioStationList: RecyclerView
    private lateinit var textPlaylistEmpty: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textRadioListTitle = view.findViewById(R.id.text_myFavorites_title)
        textRadioListTitle.text = resources.getText(R.string.currentPlaylist_title)

        textRadioListSecondTitleSelect = view.findViewById(R.id.text_radioStationDialogTitleSelect)
        textRadioListSecondTitleDownload = view.findViewById(R.id.text_radioStationDialogTitleDownload)
        textRadioListSecondTitleSelect.isVisible = false
        textRadioListSecondTitleDownload.isVisible = false

        imageArrowBack = view.findViewById(R.id.image_arrowBack)
        recyclerViewRadioStationList = view.findViewById(R.id.recyclerView_radioStationList)

        textPlaylistEmpty = view.findViewById(R.id.text_favouritesEmpty)
        textPlaylistEmpty.text = resources.getText(R.string.currentPlaylist_empty)

        // 1.2. ViewModel. We bind our viewModel to the lifecycle of our activity, not fragment. We pass our activity as an owner of the lifecycle.
        // So, we need to do this way:
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        radioStationPlaylist = mainViewModel.mediaItemsListLiveData.value?.data ?: emptyList()

        if (radioStationPlaylist.isNotEmpty()) {
//        if (!radioStationPlaylist.isNullOrEmpty()) {
            radioListAdapter = RadioListAdapter(radioStationPlaylist) { radioStationPresentationOnClick ->
                Log.d(TAG, "Выбранный элемент списка: $radioStationPresentationOnClick")

                // Открываем по клику другой фрагмент, передаём туда нашу радиостанцию
                val direction = CurrentPlaylistFragmentDirections.actionCurrentPlaylistFragmentToHomeRadioFragment(radioStationPresentationOnClick)
                if (this.findNavController().currentDestination?.id == R.id.currentPlaylistFragment) {
                    this.findNavController().navigate(direction)
                }
            }

            recyclerViewRadioStationList.adapter = radioListAdapter
        } else {
            textPlaylistEmpty.isVisible = true
        }

        // TODO Этому здесь не место. Если будет так оставлять, нужно перенести в корутины. Хотя с другой стороны - нам нужно, чтобы пока не заиграет станция, ничего не нажималось
        val isPlayerPrepared = mainViewModel.playbackStateLiveData.value?.isPrepared ?: false
        mainViewModel.playbackStateLiveData.value?.let { playbackState ->
            if (isPlayerPrepared && playbackState.isPlayEnabled) {
                musicServiceConnection.transportControls.play()
            }

        }
        initListeners()
    }

    private fun initListeners() {
        imageArrowBack.setOnClickListener {
            if (this.findNavController().currentDestination?.id == R.id.currentPlaylistFragment) {
                this.findNavController()
                    .navigate(R.id.action_currentPlaylistFragment_to_homeRadioFragment)
            }
        }
    }
}