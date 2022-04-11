package com.myproject.radiojourney.presentation.content.radioList

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.myproject.radiojourney.IAppSettings
import com.myproject.radiojourney.R
import com.myproject.radiojourney.databinding.LayoutRadioStationListBinding
import com.myproject.radiojourney.model.presentation.RadioStationPresentation
import com.myproject.radiojourney.presentation.content.base.BaseContentFragmentAbstract
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Страница со списком радиостанций по конкретной стране
 */
@AndroidEntryPoint
class RadioListFragment : BaseContentFragmentAbstract() {
    companion object {
        private const val TAG = "RadioListFragment"
    }

    @Inject
    lateinit var appSettings: IAppSettings

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: LayoutRadioStationListBinding? = null

    private val viewModel by viewModels<RadioListViewModel>()
    private lateinit var dialogInternetTrouble: Dialog
    private lateinit var countryCode: String
    private lateinit var countryName: String
    private var radioStationList = listOf(
        RadioStationPresentation("Test", "test", 2, "test", false)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = LayoutRadioStationListBinding.inflate(inflater, container, false)
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

        // Получаем результат с предыдущей страницы
        arguments?.getString("country_code")?.let { country_code_string ->
            val resultArray = country_code_string.split("||")
            countryCode = resultArray[0]
            countryName = resultArray[1]
        }
        binding?.textRadioStationDialogCity?.text = countryName

        // Получаем список радиостанций, преобразуем. Сохранять в Room не будем. Радиостанций очень много, будет занимать много места на телефоне.
        // Кроме того, списки на сервере постоянно обновляются. Возможно какой-то радиостанции в списке уже не будет, а в локальной БД она ещё осталась. Пользователь выберет её и будет ошибка.
        viewModel.getRadioStationList(countryCode)

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
        viewModel.radioStationListLiveData.observe(
            viewLifecycleOwner,
            { radioStationPresentationList ->
                radioStationList = radioStationPresentationList
                showProgress()

                // 1.5. ОБРАБОТКА КЛИКА -> Получаем результат клика во фрагменте (описываем нашу анонимную функцию из RecyclerView)
                binding?.recyclerViewRadioStationList?.adapter =
                    RadioListAdapter(radioStationList) { radioStationPresentationOnClick ->
                        Log.d(TAG, "Выбранный элемент списка: $radioStationPresentationOnClick")
                        // Открываем по клику другой фрагмент, передаём туда нашу радиостанцию
                        val direction =
                            RadioListFragmentDirections.actionRadioListFragmentToHomeRadioFragment(
                                radioStationPresentationOnClick
                            )
                        this.findNavController().navigate(direction)
                    }

                Log.d(
                    TAG,
                    "Успешный запрос в локальную БД (радиостанции). Получен результат: массив size = ${radioStationPresentationList.size}, элемент[0] = ${radioStationPresentationList[0].countryCode}, ${radioStationPresentationList[0].url}"
                )

                hideProgress()
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
        this.findNavController().navigate(R.id.action_radioListFragment_to_auth_nav_graph)
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}