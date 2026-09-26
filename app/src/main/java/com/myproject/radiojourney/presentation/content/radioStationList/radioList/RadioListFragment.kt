package com.myproject.radiojourney.presentation.content.radioStationList.radioList

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.myproject.radiojourney.R
import com.myproject.radiojourney.presentation.MainViewModel
import com.myproject.radiojourney.presentation.common.InfoDialog
import com.myproject.radiojourney.presentation.common.collectWhenStarted
import com.myproject.radiojourney.presentation.common.fallbackStationListMessage
import com.myproject.radiojourney.presentation.common.navigateSafely
import com.myproject.radiojourney.presentation.common.popBackStackSafely
import com.myproject.radiojourney.presentation.content.radioStationList.adapter.ListRadioStationAdapter
import com.myproject.radiojourney.presentation.content.radioStationList.base.BaseRadioListFragmentAbstract
import com.myproject.radiojourney.presentation.model.RadioStationPresentation
import com.myproject.radiojourney.utils.extension.startStationIndex
import dagger.hilt.android.AndroidEntryPoint

/**
 * Экран со списком станций одной страны (открывается с карты).
 *
 * Пока плейлист этой страны не загружен в плеер, станции выбрать нельзя: вверху надпись "скачать и включить".
 * Если сервер недоступен, а список этой страны уже скачивали раньше, показывается сохранённый - с сообщением,
 * когда он был сохранён (см. RadioStationList)
 */
@AndroidEntryPoint
class RadioListFragment : BaseRadioListFragmentAbstract() {

    // ViewModel плейера привязана к Activity, а не к фрагменту
    private val mainViewModel by activityViewModels<MainViewModel>()

    private val viewModel by viewModels<RadioListViewModel>()

    private lateinit var infoDialog: InfoDialog
    private lateinit var textRadioListSecondTitleSelect: AppCompatTextView
    private lateinit var textRadioListSecondTitleDownload: AppCompatTextView
    private lateinit var textRadioStationsEmpty: TextView
    private lateinit var recyclerViewRadioStationList: RecyclerView
    private val listRadioStationAdapter = ListRadioStationAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<AppCompatTextView>(R.id.text_myFavorites_title).text =
            viewModel.countryName

        textRadioListSecondTitleSelect = view.findViewById(R.id.text_radioStationDialogTitleSelect)
        textRadioListSecondTitleSelect.isVisible = true
        textRadioListSecondTitleSelect.text = getString(R.string.radioStationList_title_loading)

        textRadioListSecondTitleDownload =
            view.findViewById(R.id.text_radioStationDialogTitleDownload)
        textRadioStationsEmpty = view.findViewById(R.id.text_radioStationsEmpty)
        recyclerViewRadioStationList = view.findViewById(R.id.recyclerView_radioStationList)

        infoDialog = InfoDialog(requireContext())
        infoDialog.showIfNoInternet()

        recyclerViewRadioStationList.adapter = listRadioStationAdapter
        listRadioStationAdapter.setItemClickListener { radioStation ->
            // Открываем по клику другой фрагмент, передаём туда нашу радиостанцию
            openHomeRadio(radioStation)
        }

        // Назад на карту - так же, как системная кнопка "Назад" (см. комментарий в app_navigation.xml)
        view.findViewById<AppCompatImageView>(R.id.image_arrowBack)
            .setOnClickListener { popBackStackSafely() }

        subscribeOnFlow()
    }

    private fun subscribeOnFlow() {
        viewLifecycleOwner.collectWhenStarted(viewModel.uiState) { uiState ->
            val radioStationList = when (uiState) {
                RadioListViewModel.UiState.Loading -> return@collectWhenStarted
                is RadioListViewModel.UiState.ServerIsDown -> {
                    // Диалоговое окно при ошибке сервера: текст зависит от причины (нет сети, сервер молчит, ответ обрывается)
                    infoDialog.showServerError(uiState.reason)
                    return@collectWhenStarted
                }

                is RadioListViewModel.UiState.Loaded -> {
                    // Список запасной (сохранённый или только популярные станции) - говорим об этом, не закрывая список
                    requireContext().fallbackStationListMessage(
                        uiState.savedAt,
                        uiState.isOnlyPopular,
                        uiState.radioStations.size
                    )?.let { message ->
                        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
                            .setTextMaxLines(4).show()
                    }
                    uiState.radioStations
                }
            }

            if (radioStationList.isEmpty()) {
                textRadioStationsEmpty.isVisible = true
                textRadioListSecondTitleSelect.isVisible = false
                textRadioListSecondTitleDownload.isVisible = false
                return@collectWhenStarted
            }

            textRadioStationsEmpty.isVisible = false
            listRadioStationAdapter.radioStationList = radioStationList
            changeTextSelectOrDownload(radioStationList)

            textRadioListSecondTitleDownload.setOnClickListener {
                // Иногда после скачивания нового плейлиста экзоплейер не обновляется. Поэтому перед тем, как включить первую станцию нового плейлиста, укажем явно, что его нужно скачать
                mainViewModel.fetchSongs(radioStationList[0].countryCode)
                // Передаём станцию, с которой начнётся плейлист (самую популярную - ту же выберет MainActivity).
                // Для неё же отправляется отметка "популярная" на сервер radio-browser
                openHomeRadio(radioStationList[radioStationList.startStationIndex()])
            }
        }
    }

    // Если у нас играет другой плейлист, нужно показать надпись "скачать". Если же этот плейлист уже скачан - "выберите радиостанцию"
    private fun changeTextSelectOrDownload(radioStationList: List<RadioStationPresentation>) {
        val countryCodeInPlayer = mainViewModel.currentPlaylistStations.firstOrNull()?.countryCode
        val isPlaylistInPlayer = radioStationList[0].countryCode == countryCodeInPlayer

        textRadioListSecondTitleSelect.isVisible = isPlaylistInPlayer
        textRadioListSecondTitleSelect.text = getString(R.string.radioStationList_title_select)
        textRadioListSecondTitleDownload.isVisible = !isPlaylistInPlayer
        listRadioStationAdapter.isClickableRecyclerView = isPlaylistInPlayer
    }

    private fun openHomeRadio(radioStation: RadioStationPresentation) {
        navigateSafely(
            RadioListFragmentDirections.actionRadioListFragmentToHomeRadioFragment(
                radioStation
            )
        )
    }

    override fun onDestroyView() {
        infoDialog.dismiss() // Открытый диалог закрываем вместе с экраном, иначе WindowLeaked
        super.onDestroyView()
    }
}