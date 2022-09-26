package com.myproject.radiojourney.presentation.content.recommendedList

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.myproject.radiojourney.IAppSettings
import com.myproject.radiojourney.R
import com.myproject.radiojourney.databinding.LayoutRadioStationListRecommendedBinding
import com.myproject.radiojourney.other.Status
import com.myproject.radiojourney.presentation.content.base.BaseContentFragmentAbstract
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Страница рекомендуемых радиостанций
 */
@AndroidEntryPoint
class RecommendedListFragment : BaseContentFragmentAbstract() {
    companion object {
        private const val TAG = "RecommendedListFragment"
    }

    @Inject
    lateinit var appSettings: IAppSettings

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: LayoutRadioStationListRecommendedBinding? = null

    private val viewModel by viewModels<RecommendedListViewModel>()
    private lateinit var dialogInternetTrouble: Dialog
    private lateinit var recommendedListAdapter: RecommendedListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = LayoutRadioStationListRecommendedBinding.inflate(inflater, container, false)
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

        // Получаем список рекомендуемого для отображения
        viewModel.getRadioStationRecommendedListAndShow()

        // Настройки диалогового окна
        dialogInternetTrouble = Dialog(requireContext())
        // Передайте ссылку на разметку
        dialogInternetTrouble.setContentView(R.layout.layout_internet_trouble_dialog)

        subscribeOnLiveData()
    }

    private fun subscribeOnLiveData() {
        // Показываем или прячем Progress
        viewModel.showProgressLiveData.observe(viewLifecycleOwner, {
            showProgress()
        })
        viewModel.hideProgressLiveData.observe(viewLifecycleOwner, {
            hideProgress()
        })
        viewModel.dialogInternetTroubleLiveData.observe(viewLifecycleOwner, {
            dialogInternetTrouble.show()
        })
        viewModel.errorMessageLiveData.observe(this) {
            it?.getContentIfNotHandled()?.let { result ->
                when (result.status) {
                    // If everything is ok, we don't want to show anything. Only if smth went wrong
                    Status.ERROR ->
                        binding?.let { nonNullBinding ->
                            Snackbar.make(
                                nonNullBinding.recyclerViewRecommendedRadioStationList.rootView,
                                result.message ?: "An unknown error occurred",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    else -> Unit
                }
            }
        }
        viewModel.radioStationRecommendedListLiveData.observe(
            viewLifecycleOwner,
            { radioStationPresentationList ->
                showProgress()

                // 1.5. ОБРАБОТКА КЛИКА -> Получаем результат клика во фрагменте (описываем нашу анонимную функцию из RecyclerView)
                // Инициализация адаптера
                if (radioStationPresentationList != null && radioStationPresentationList.isNotEmpty()) {

                    recommendedListAdapter = RecommendedListAdapter(
                        radioStationPresentationList,
                        { radioStationPresentationOnClick ->
                            Log.d(TAG, "Выбранный элемент списка: $radioStationPresentationOnClick")
                            // Открываем по клику другой фрагмент, передаём туда нашу радиостанцию
                            val direction =
                                RecommendedListFragmentDirections.actionRecommendedListFragmentToHomeRadioFragment(
                                    radioStationPresentationOnClick
                                )
                            this.findNavController().navigate(direction)
                        },
                        { radioStationOnStarClick ->
                            Log.d(
                                TAG,
                                "Выбранный элемент списка: $radioStationOnStarClick"
                            )
                            // По клику нужно добавить либо удалить из избранного, предварительно проверив наличие радиостанции в базе
                            viewModel.checkIsStationInFavouritesAndChangeTheStar(
                                radioStationOnStarClick
                            )
                        })
                    binding?.recyclerViewRecommendedRadioStationList?.adapter =
                        recommendedListAdapter

                    val animator: DefaultItemAnimator = object : DefaultItemAnimator() {
                        override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean {
                            return true
                        }
                    }
                    binding?.recyclerViewRecommendedRadioStationList?.itemAnimator = animator

                } else {
                    hideProgress()
                    return@observe
                }

                Log.d(
                    TAG,
                    "Успешный запрос в локальную БД (радиостанции). Получен результат: массив size = ${radioStationPresentationList.size}, элемент[0] = ${radioStationPresentationList[0].countryCode}, ${radioStationPresentationList[0].urlResolved}"
                )

                hideProgress()
            })
        viewModel.stationSavedInFavouritesLiveData.observe(viewLifecycleOwner, {
            binding?.recyclerViewRecommendedRadioStationList?.adapter?.notifyDataSetChanged()
        })
        viewModel.stationDeletedFromFavouritesLiveData.observe(viewLifecycleOwner, {
            binding?.recyclerViewRecommendedRadioStationList?.adapter?.notifyDataSetChanged()
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
//        this.findNavController().navigate(R.id.action_recommendedListFragment_to_auth_nav_graph)
        activity?.finish()
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}