package com.myproject.radiojourney.presentation.content.radioStationList.favourite

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.myproject.radiojourney.R
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import com.myproject.radiojourney.presentation.MainViewModel
import com.myproject.radiojourney.presentation.common.InfoDialog
import com.myproject.radiojourney.presentation.content.radioStationList.base.BaseRadioListFragmentAbstract
import dagger.hilt.android.AndroidEntryPoint

/**
 * Страница с избранным
 */
@AndroidEntryPoint
class FavouriteListFragment : BaseRadioListFragmentAbstract() {

    private val viewModel by viewModels<FavouriteListViewModel>()

    // ViewModel плейера привязана к Activity, а не к фрагменту
    private val mainViewModel by activityViewModels<MainViewModel>()

    private lateinit var infoDialog: InfoDialog
    private lateinit var textRadioListSecondTitleDownload: AppCompatTextView
    private lateinit var textFavouritesEmpty: TextView

    // Адаптер создаётся вместе с фрагментом (а не в наблюдателе списка). Раньше он был lateinit и создавался только
    // после загрузки списка, а changeTextDownloadOrNothing() обращался к нему раньше - например, после пересоздания экрана
    private val favouriteListAdapter = FavoriteListAdapter(
        onItemClicked = { radioStation -> openHomeRadio(radioStation) },
        // По клику нужно добавить либо удалить из избранного
        onStarClicked = { radioStation -> viewModel.checkIsStationInFavouritesAndChangeTheStar(radioStation) }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<AppCompatTextView>(R.id.text_myFavorites_title).text = getString(R.string.favouriteRadioStationList_title)
        view.findViewById<AppCompatTextView>(R.id.text_radioStationDialogTitleSelect).isVisible = false
        textRadioListSecondTitleDownload = view.findViewById(R.id.text_radioStationDialogTitleDownload)
        textFavouritesEmpty = view.findViewById(R.id.text_favouritesEmpty)

        val recyclerViewRadioStationList = view.findViewById<RecyclerView>(R.id.recyclerView_radioStationList)
        recyclerViewRadioStationList.adapter = favouriteListAdapter
        // Без мигания элемента при смене звезды
        recyclerViewRadioStationList.itemAnimator = object : DefaultItemAnimator() {
            override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean = true
        }

        infoDialog = InfoDialog(requireContext())
        infoDialog.showIfNoInternet()

        val goToHomeRadio = View.OnClickListener {
            if (findNavController().currentDestination?.id == R.id.favouriteListFragment) {
                findNavController().navigate(R.id.action_favouriteListFragment_to_homeRadioFragment)
            }
        }
        view.findViewById<AppCompatImageView>(R.id.image_arrowBack).setOnClickListener(goToHomeRadio)
        textFavouritesEmpty.setOnClickListener(goToHomeRadio)

        subscribeOnLiveData()
    }

    private fun subscribeOnLiveData() {
        viewModel.radioStationFavouriteListLiveData.observe(viewLifecycleOwner) { favouriteStationList ->
            favouriteListAdapter.favouriteStationList = favouriteStationList
            textFavouritesEmpty.isVisible = favouriteStationList.isEmpty()
            changeTextDownloadOrNothing(favouriteStationList)

            textRadioListSecondTitleDownload.setOnClickListener {
                // Иногда после скачивания нового плейлиста экзоплейер не обновляется. Поэтому перед тем, как включить первую станцию нового плейлиста, укажем явно, что его нужно скачать
                mainViewModel.fetchSongs("FAV")
                // Открываем по клику другой фрагмент, передаём туда первую станцию
                openHomeRadio(favouriteStationList[0])
            }
        }

        // Звезду нажали в этом списке - сообщаем плейеру (и остальным экранам), какая именно станция изменилась
        viewModel.stationFavouriteChangedLiveData.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { station ->
                mainViewModel.notifyFavouriteChanged(station, station.isStationInFavourite)
            }
        }

        // Звезду нажали в плейере, пока открыт этот список: меняем звезду у той же станции или добавляем станцию в список.
        // Изменения, случившиеся до открытия списка, пропускаем - список и так загружается из базы уже с ними
        val skipFavouriteChangesUpToId = mainViewModel.lastFavouriteChangeIdForNewObserver
        mainViewModel.favouriteChangeLiveData.observe(viewLifecycleOwner) { change ->
            if (change.id <= skipFavouriteChangesUpToId) return@observe
            viewModel.applyFavouriteChangeFromOutside(change.station, change.isFavourite)
        }
    }

    // Если в плейере не плейлист избранного, показываем надпись "скачать" и не даём выбрать станцию.
    // В этом фрагменте могут быть только избранные радиостанции, поэтому проверять можно только список в плейере
    private fun changeTextDownloadOrNothing(favouriteStationList: List<RadioStationPresentation>) {
        val countryCodeInPlayer = mainViewModel.mediaItemsListLiveData.value?.data?.firstOrNull()?.countryCode.orEmpty()
        val isFavouritesInPlayer = countryCodeInPlayer.endsWith("_FAV", ignoreCase = true)

        textRadioListSecondTitleDownload.isVisible = favouriteStationList.isNotEmpty() && !isFavouritesInPlayer
        favouriteListAdapter.isClickableRecyclerView = isFavouritesInPlayer
    }

    private fun openHomeRadio(radioStation: RadioStationPresentation) {
        if (findNavController().currentDestination?.id == R.id.favouriteListFragment) {
            findNavController().navigate(
                FavouriteListFragmentDirections.actionFavouriteListFragmentToHomeRadioFragment(radioStation)
            )
        }
    }

    override fun onDestroyView() {
        infoDialog.dismiss() // Открытый диалог закрываем вместе с экраном, иначе WindowLeaked
        super.onDestroyView()
    }
}