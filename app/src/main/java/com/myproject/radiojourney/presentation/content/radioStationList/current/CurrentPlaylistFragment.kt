package com.myproject.radiojourney.presentation.content.radioStationList.current

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
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

//    @Inject
//    lateinit var musicServiceConnection: MusicServiceConnection // comment - 006 claude

    private var radioStationPlaylist: List<RadioStationPresentation> = emptyList()
    private lateinit var textRadioListTitle: AppCompatTextView
    private lateinit var textRadioListSecondTitleSelect: AppCompatTextView
    private lateinit var textRadioListSecondTitleDownload: AppCompatTextView
    private lateinit var imageArrowBack: AppCompatImageView
    private lateinit var radioListAdapter: RadioListAdapter
    private lateinit var recyclerViewRadioStationList: RecyclerView
    private lateinit var textPlaylistEmpty: TextView
    private lateinit var dialogInternetTrouble: Dialog
    private var isInternetAvailable = false

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

        // Настройки диалогового окна
        dialogInternetTrouble = Dialog(requireContext())
        // Передайте ссылку на разметку
        dialogInternetTrouble.setContentView(R.layout.layout_internet_trouble_dialog)

        activity?.let{
            isInternetAvailable = mainViewModel.isInternetAvailable(it)
            if (!isInternetAvailable) {
                // Диалоговое окно при отсутствии интернета
                val textInternetTrouble = getString(R.string.dialogInternetTrouble_text3)
                val textViewInternetTrouble =
                    dialogInternetTrouble.findViewById<AppCompatTextView>(R.id.text_internetTrouble)
                textViewInternetTrouble.text = textInternetTrouble

                dialogInternetTrouble.show()
            }
        }

        radioStationPlaylist = mainViewModel.mediaItemsListLiveData.value?.data ?: emptyList()

        if (radioStationPlaylist.isNotEmpty()) {
//        if (!radioStationPlaylist.isNullOrEmpty()) {

            // <!-- 005 claude
//            radioListAdapter = RadioListAdapter(radioStationPlaylist) { radioStationPresentationOnClick ->

            val currentStationUuid = mainViewModel.curPlayingSongLiveData.value?.description?.mediaId
            radioListAdapter = RadioListAdapter(radioStationPlaylist, currentStationUuid) { radioStationPresentationOnClick ->
            // 005 claude -->

                Log.d(TAG, "Выбранный элемент списка: $radioStationPresentationOnClick")

                // Открываем по клику другой фрагмент, передаём туда нашу радиостанцию
                val direction = CurrentPlaylistFragmentDirections.actionCurrentPlaylistFragmentToHomeRadioFragment(radioStationPresentationOnClick)
                if (this.findNavController().currentDestination?.id == R.id.currentPlaylistFragment) {
                    this.findNavController().navigate(direction)
                }
            }

            recyclerViewRadioStationList.adapter = radioListAdapter

            // <!-- 005 claude
            // Открываем список там, где сейчас играющая станция (порядок станций не меняется)
            scrollToStation(radioListAdapter.indexOf(currentStationUuid))

            // Если станция переключилась, пока список открыт (например, кнопкой в уведомлении), переносим выделение
            mainViewModel.curPlayingSongLiveData.observe(viewLifecycleOwner) { metadata ->
                radioListAdapter.setCurrentStation(metadata?.description?.mediaId)
            }
            // 005 claude -->

        } else {
            textPlaylistEmpty.isVisible = true
        }

        // <!-- 006 claude
//        // TODO Этому здесь не место. Если будет так оставлять, нужно перенести в корутины. Хотя с другой стороны - нам нужно, чтобы пока не заиграет станция, ничего не нажималось. Пока что всё и так работает хорошо
//        val isPlayerPrepared = mainViewModel.playbackStateLiveData.value?.isPrepared ?: false
//        mainViewModel.playbackStateLiveData.value?.let { playbackState ->
//            if (isPlayerPrepared && playbackState.isPlayEnabled) {
//                musicServiceConnection.transportControls.play()
//            }
//
//        }

        // Раньше здесь плейер автоматически включался при открытии списка (обходили глюки переключения станций).
        // Теперь, если плейер на паузе, он остаётся на паузе
        // 006 claude -->

        initListeners()
    }

    // <!-- 005 claude
    // Прокручиваем список к станции. Станцию показываем примерно на трети высоты списка, чтобы были видны и соседние
    private fun scrollToStation(position: Int) {
        if (position < 0) return
        // post: прокрутка, вызванная прямо во время первой отрисовки списка, игнорируется
        recyclerViewRadioStationList.doOnLayout {
            recyclerViewRadioStationList.post {
                (recyclerViewRadioStationList.layoutManager as? LinearLayoutManager)
                    ?.scrollToPositionWithOffset(position, recyclerViewRadioStationList.height / 3)
            }
        }
    }
    // 005 claude -->

    private fun initListeners() {
        imageArrowBack.setOnClickListener {
            if (this.findNavController().currentDestination?.id == R.id.currentPlaylistFragment) {
                this.findNavController()
                    .navigate(R.id.action_currentPlaylistFragment_to_homeRadioFragment)
            }
        }
    }
}