package com.myproject.radiojourney.presentation.content.radioStationList.favourite

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.myproject.radiojourney.R
import dagger.hilt.android.AndroidEntryPoint
import androidx.recyclerview.widget.DefaultItemAnimator
import com.google.android.material.snackbar.Snackbar
import com.myproject.radiojourney.other.Status
import com.myproject.radiojourney.presentation.MainViewModel
import com.myproject.radiojourney.presentation.content.radioStationList.base.BaseRadioListFragmentAbstract
import com.myproject.radiojourney.presentation.content.radioStationList.radioList.RadioListFragment
import com.myproject.radiojourney.presentation.content.radioStationList.radioList.RadioListFragmentDirections
import java.io.IOException

/**
 * Страница с избранным
 */
@AndroidEntryPoint
class FavouriteListFragment : BaseRadioListFragmentAbstract() {
    companion object {
        private const val TAG = "FavouriteListFragment"
    }

    private val viewModel by viewModels<FavouriteListViewModel>()

    // 1.1. ViewModel. We bind our viewModel to the cycle of our activity, not fragment. So, we need to do this way:
    private lateinit var mainViewModel: MainViewModel

    private lateinit var dialogInternetTrouble: Dialog
    private lateinit var favouriteListAdapter: FavoriteListAdapter
    private lateinit var textRadioListTitle: AppCompatTextView
    private lateinit var textRadioListSecondTitleSelect: AppCompatTextView
    private lateinit var textRadioListSecondTitleDownload: AppCompatTextView
    private lateinit var imageArrowBack: AppCompatImageView
    private lateinit var textFavouritesEmpty: TextView
    private lateinit var recyclerViewRadioStationList: RecyclerView
    private lateinit var frameLayout: FrameLayout
    private lateinit var progressCircular: ProgressBar
    private lateinit var radioCountryCodeFromActivity: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1.2. ViewModel. We bind our viewModel to the lifecycle of our activity, not fragment. We pass our activity as an owner of the lifecycle.
        // So, we need to do this way:
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        textRadioListTitle = view.findViewById(R.id.text_myFavorites_title)
        textRadioListTitle.text = resources.getText(R.string.favouriteRadioStationList_title)

        textRadioListSecondTitleSelect = view.findViewById(R.id.text_radioStationDialogTitleSelect)
        textRadioListSecondTitleDownload =
            view.findViewById(R.id.text_radioStationDialogTitleDownload)
        textRadioListSecondTitleSelect.isVisible = false

        imageArrowBack = view.findViewById(R.id.image_arrowBack)
        textFavouritesEmpty = view.findViewById(R.id.text_favouritesEmpty)
        recyclerViewRadioStationList = view.findViewById(R.id.recyclerView_radioStationList)
        frameLayout = view.findViewById(R.id.frameLayout)
        progressCircular = view.findViewById(R.id.progressCircular)

        // Получаем список избранного для отображения
        viewModel.getRadioStationFavouriteListAndShow()

        // Настройки диалогового окна
        dialogInternetTrouble = Dialog(requireContext())
        // Передайте ссылку на разметку
        dialogInternetTrouble.setContentView(R.layout.layout_internet_trouble_dialog)

        initListeners()
        subscribeOnLiveData()

        // Если у нас играет другой плейлист, нужно показать надпись "скачать". Если же этот плейлист уже скачан - "выберите радиостанцию"
        radioCountryCodeFromActivity =
            if ((mainViewModel.mediaItemsListLiveData.value?.data?.size ?: 0) >= 1) {
                mainViewModel.mediaItemsListLiveData.value?.data?.get(0)?.countryCode
                    ?: ""
            } else {
                ""
            }
        changeTextDownloadOrNothing()
    }

    private fun initListeners() {
        imageArrowBack.setOnClickListener {
            if (this.findNavController().currentDestination?.id == R.id.favouriteListFragment) {
                this.findNavController()
                    .navigate(R.id.action_favouriteListFragment_to_homeRadioFragment)
            }
        }
        textFavouritesEmpty.setOnClickListener {
            if (this.findNavController().currentDestination?.id == R.id.favouriteListFragment) {
                this.findNavController()
                    .navigate(R.id.action_favouriteListFragment_to_homeRadioFragment)
            }
        }
    }

    private fun subscribeOnLiveData() {
        // Показываем или прячем Progress
        viewModel.showProgressLiveData.observe(viewLifecycleOwner) {
            showProgress()
        }
        viewModel.hideProgressLiveData.observe(viewLifecycleOwner) {
            hideProgress()
        }
        viewModel.dialogInternetTroubleLiveData.observe(viewLifecycleOwner) {
            dialogInternetTrouble.show()
        }
        viewModel.errorMessageLiveData.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandled()?.let { result ->
                when (result.status) {
                    // If everything is ok, we don't want to show anything. Only if smth went wrong
                    Status.ERROR ->
                        view?.let { nonNullBinding ->
                            Snackbar.make(
                                nonNullBinding.rootView,
                                result.message ?: "An unknown error occurred",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }

                    else -> Unit
                }
            }
        }
        viewModel.radioStationFavouriteListLiveData.observe(viewLifecycleOwner) { radioStationFavouritePresentationList ->
            showProgress()

            // 1.5. ОБРАБОТКА КЛИКА -> Получаем результат клика во фрагменте (описываем нашу анонимную функцию из RecyclerView)
            // Инициализация адаптера
            val favouriteStationList = viewModel.radioStationFavouriteListLiveData.value

            if (!favouriteStationList.isNullOrEmpty()) {

                favouriteListAdapter = FavoriteListAdapter(
                    favouriteStationList,
                    { radioStationFavouriteOnClick ->
                        Log.d(TAG, "Выбранный элемент списка: $radioStationFavouriteOnClick")
                        // Открываем по клику другой фрагмент, передаём туда нашу радиостанцию
                        val direction =
                            FavouriteListFragmentDirections.actionFavouriteListFragmentToHomeRadioFragment(
                                radioStationFavouriteOnClick
                            )
                        if (this.findNavController().currentDestination?.id == R.id.favouriteListFragment) {
                            this.findNavController().navigate(direction)
                        }
                    },
                    { radioStationFavouriteOnStarClick ->
                        Log.d(
                            TAG,
                            "Выбранный элемент списка: $radioStationFavouriteOnStarClick"
                        )
                        // По клику нужно добавить либо удалить из избранного, предварительно проверив наличие радиостанции в базе
                        viewModel.checkIsStationInFavouritesAndChangeTheStar(
                            radioStationFavouriteOnStarClick
                        )
                    })

                recyclerViewRadioStationList.adapter = favouriteListAdapter

                val animator: DefaultItemAnimator = object : DefaultItemAnimator() {
                    override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean {
                        return true
                    }
                }
                recyclerViewRadioStationList.itemAnimator = animator

                changeTextDownloadOrNothing()

                textRadioListSecondTitleDownload.setOnClickListener {
                    Log.d(TAG, "Загружаем плейлист")

                    // Иногда после скачивания нового плейлиста экзоплейер не обновляется. Поэтому перед тем, как включить первую станцию нового плейлиста, укажем явно, что его нужно скачать
                    mainViewModel.fetchSongs("FAV")

                    // Открываем по клику другой фрагмент, передаём туда нашу радиостанцию
                    val direction =
                        FavouriteListFragmentDirections.actionFavouriteListFragmentToHomeRadioFragment(
                            radioStationFavouritePresentationList[0]
                        )
                    if (this.findNavController().currentDestination?.id == R.id.favouriteListFragment) {
                        this.findNavController().navigate(direction)
                    }
                }

            } else {
                textFavouritesEmpty.isVisible = true
                hideProgress()
                return@observe
            }

            Log.d(
                TAG,
                "Успешный запрос в локальную БД (радиостанции). Получен результат: массив size = ${radioStationFavouritePresentationList.size}, элемент[0] = ${radioStationFavouritePresentationList[0].countryCode}, ${radioStationFavouritePresentationList[0].urlResolved}"
            )

            hideProgress()
        }
        viewModel.stationSavedInFavouritesLiveData.observe(viewLifecycleOwner) {
            recyclerViewRadioStationList.adapter?.notifyDataSetChanged()
            // Нужно так же сообщить это плейеру в activity
            mainViewModel.changeTheStar(true)
        }
        viewModel.stationDeletedFromFavouritesLiveData.observe(viewLifecycleOwner) {
            recyclerViewRadioStationList.adapter?.notifyDataSetChanged()
            // Нужно так же сообщить это плейеру в activity
            mainViewModel.changeTheStar(false)
        }

        // Если изменение было в activity, и открыт этот фрагмент, здесь тоже нужно это отобразить:
        mainViewModel.stationSavedInFavouritesLiveData.observe(viewLifecycleOwner) {
            recyclerViewRadioStationList.adapter?.notifyDataSetChanged()
            viewModel.changeTheStar(
                mainViewModel.curPlayingSongLiveData.value?.description?.mediaId,
                true
            )
        }
        // Если мы добавили звезду в плейере, то в список в FavouriteListFragment нужно добавить не просто звезду, а всю позицию - на случай, если её там не было
        mainViewModel.addAStationToFavouriteListIfItIsNotThereLiveData.observe(viewLifecycleOwner) {
            viewModel.addAStationToFavouriteListIfItIsNotThere(it)
        }
        // Если добавляем первую станцию в пустой список, нужно убрать надпись
        viewModel.addingAStationToAnEmptyListLiveData.observe(viewLifecycleOwner) {
            textFavouritesEmpty.isVisible = false
        }
        mainViewModel.stationDeletedFromFavouritesLiveData.observe(viewLifecycleOwner) {
            recyclerViewRadioStationList.adapter?.notifyDataSetChanged()
            viewModel.changeTheStar(
                mainViewModel.curPlayingSongLiveData.value?.description?.mediaId,
                false
            )
        }
    }

    private fun changeTextDownloadOrNothing() {
        val favouriteStationList = viewModel.radioStationFavouriteListLiveData.value

        // Список избранного может быть пустым. Проверяем
        if (!favouriteStationList.isNullOrEmpty()) {

            // Если он не пуст, всё хорошо. В этом фрагменте могут быть только избранные радиостанции, поэтому проверять можно только список в плейере
//            val radioCountryCodeFromFragment = favouriteStationList[0].countryCode

            // textRadioListSecondTitleDownload виден только если у нас список избранного и на экране, и в плейере:
//            textRadioListSecondTitleDownload.isVisible =
//                !radioCountryCodeFromActivity.endsWith("_FAV", ignoreCase = true)
            try {
                if (radioCountryCodeFromActivity.endsWith("_FAV", ignoreCase = true)) {
                    textRadioListSecondTitleDownload.isVisible = false
                    favouriteListAdapter.isClickableRecyclerView = true
                } else {
                    textRadioListSecondTitleDownload.isVisible = true
                    favouriteListAdapter.isClickableRecyclerView = false
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

        } else {
            // Favourites list is empty
            textRadioListSecondTitleDownload.isVisible = false
        }
    }

    private fun showProgress() {
        frameLayout.isVisible = true
        progressCircular.isVisible = true
    }

    private fun hideProgress() {
        frameLayout.isVisible = false
        progressCircular.isVisible = false
    }
}