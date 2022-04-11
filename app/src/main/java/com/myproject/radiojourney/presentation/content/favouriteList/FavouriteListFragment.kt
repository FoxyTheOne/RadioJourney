package com.myproject.radiojourney.presentation.content.favouriteList

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.myproject.radiojourney.IAppSettings
import com.myproject.radiojourney.R
import com.myproject.radiojourney.presentation.content.base.BaseContentFragmentAbstract
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.recyclerview.widget.DefaultItemAnimator
import com.myproject.radiojourney.databinding.LayoutRadioStationListFavouriteBinding

/**
 * Страница с избранным
 */
@AndroidEntryPoint
class FavouriteListFragment : BaseContentFragmentAbstract() {
    companion object {
        private const val TAG = "FavouriteListFragment"
    }

    @Inject
    lateinit var appSettings: IAppSettings

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: LayoutRadioStationListFavouriteBinding? = null

    private val viewModel by viewModels<FavouriteListViewModel>()
    private lateinit var favouriteListAdapter: FavouiteListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = LayoutRadioStationListFavouriteBinding.inflate(inflater, container, false)
        // TOOLBAR
        setHasOptionsMenu(true)
        // TOOLBAR - где будет находиться в нашем layout
        binding?.let {
            appSettings.setToolbar(it.homeToolbar)
        }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получаем список избранного для отображения
        viewModel.getRadioStationFavouriteListAndShow()

        initListeners()
        subscribeOnLiveData()
    }

    private fun initListeners() {
        binding?.textFavouritesEmpty?.setOnClickListener {
            this.findNavController()
                .navigate(R.id.action_favouriteListFragment_to_homeRadioFragment)
        }
    }

    private fun subscribeOnLiveData() {
        // Показываем или прячем Progress
        viewModel.showProgressLiveData.observe(viewLifecycleOwner, {
            showProgress()
        })
        viewModel.hideProgressLiveData.observe(viewLifecycleOwner, {
            hideProgress()
        })
        viewModel.favouritesFailedLiveData.observe(viewLifecycleOwner, {
            Toast.makeText(
                context,
                "Interacting with favourites failed. Smth wrong with your token. Try re-login.",
                Toast.LENGTH_LONG
            ).show()
        })
        viewModel.failedLiveData.observe(viewLifecycleOwner, {
            Toast.makeText(context, "Failure. Something went wrong", Toast.LENGTH_LONG).show()
        })
        viewModel.radioStationFavouriteListLiveData.observe(
            viewLifecycleOwner,
            { radioStationFavouritePresentationList ->
                showProgress()

                // 1.5. ОБРАБОТКА КЛИКА -> Получаем результат клика во фрагменте (описываем нашу анонимную функцию из RecyclerView)
                // Инициализация адаптера
                val favouriteStationList = viewModel.radioStationFavouriteListLiveData.value

                if (favouriteStationList != null && favouriteStationList.isNotEmpty()) {

                    favouriteListAdapter = FavouiteListAdapter(
                        favouriteStationList,
                        { radioStationFavouriteOnClick ->
                            Log.d(TAG, "Выбранный элемент списка: $radioStationFavouriteOnClick")
                            // Открываем по клику другой фрагмент, передаём туда нашу радиостанцию
                            val direction =
                                FavouriteListFragmentDirections.actionFavouriteListFragmentToHomeRadioFragment(
                                    radioStationFavouriteOnClick
                                )
                            this.findNavController().navigate(direction)
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

                    binding?.recyclerViewRadioStationList?.adapter = favouriteListAdapter

                    val animator: DefaultItemAnimator = object : DefaultItemAnimator() {
                        override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean {
                            return true
                        }
                    }
                    binding?.recyclerViewRadioStationList?.itemAnimator = animator

                } else {
                    binding?.textFavouritesEmpty?.isVisible = true
                    hideProgress()
                    return@observe
                }

                Log.d(
                    TAG,
                    "Успешный запрос в локальную БД (радиостанции). Получен результат: массив size = ${radioStationFavouritePresentationList.size}, элемент[0] = ${radioStationFavouritePresentationList[0].countryCode}, ${radioStationFavouritePresentationList[0].url}"
                )

                hideProgress()
            })
        viewModel.stationSavedInFavouritesLiveData.observe(viewLifecycleOwner, {
            binding?.recyclerViewRadioStationList?.adapter?.notifyDataSetChanged()
        })
        viewModel.stationDeletedFromFavouritesLiveData.observe(viewLifecycleOwner, {
            binding?.recyclerViewRadioStationList?.adapter?.notifyDataSetChanged()
        })
    }

    private fun showProgress() {
        binding?.frameLayout?.isVisible = true
        binding?.progressCircular?.isVisible = true
    }

    private fun hideProgress() {
        binding?.frameLayout?.isVisible = false
        binding?.progressCircular?.isVisible = false
    }

    // TOOLBAR
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.home_toolbar_menu, menu)
    }

    // TOOLBAR - обработка клика
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.log_out -> {
            showLogoutDialog()
            Log.d(TAG, "showLogoutDialog() was called")
            true
        }
        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            Log.d(TAG, "else result")
            super.onOptionsItemSelected(item)
        }
    }

    // TOOLBAR - Описываем метод из интерфейса ILogOutListener для выхода из аккаунта приложения
    override fun onLogOut() {
        viewModel.logout()
        this.findNavController().navigate(R.id.action_favouriteListFragment_to_auth_nav_graph)
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}