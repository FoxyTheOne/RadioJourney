package com.myproject.radiojourney.presentation.content.radioStationList.radioList

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.myproject.radiojourney.R
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import com.myproject.radiojourney.other.Status
import com.myproject.radiojourney.presentation.MainViewModel
import com.myproject.radiojourney.presentation.content.radioStationList.adapter.ListRadioStationAdapter
import com.myproject.radiojourney.presentation.content.radioStationList.base.BaseRadioListFragmentAbstract
import dagger.hilt.android.AndroidEntryPoint

/**
 * Страница со списком радиостанций по конкретной стране
 */
@AndroidEntryPoint
class RadioListFragment : BaseRadioListFragmentAbstract() {
    companion object {
        private const val TAG = "RadioListFragment"
    }

    // 1.1. ViewModel. We bind our viewModel to the cycle of our activity, not fragment. So, we need to do this way:
    private lateinit var mainViewModel: MainViewModel

    private val viewModel by viewModels<RadioListViewModel>()

    private var radioStationList = listOf(
        RadioStationPresentation(
            "2", "Test", "test", 2, "test", "test",
            isStationInFavourite = false,
            isStationInRecommended = false
        )
    )

    private lateinit var dialogInternetTrouble: Dialog
    private lateinit var countryCode: String
    private lateinit var countryName: String
    private lateinit var textRadioListTitle: AppCompatTextView
    private lateinit var textRadioListSecondTitleSelect: AppCompatTextView
    private lateinit var textRadioListSecondTitleDownload: AppCompatTextView
    private lateinit var imageArrowBack: AppCompatImageView
    private lateinit var textRadioStationsEmpty: TextView
    private lateinit var recyclerViewRadioStationList: RecyclerView
    private lateinit var frameLayout: FrameLayout
    private lateinit var progressCircular: ProgressBar
    private lateinit var radioCountryCodeFromActivity: String
    private lateinit var listRadioStationAdapter: ListRadioStationAdapter
    private var isInternetAvailable = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получаем результат с предыдущей страницы
        arguments?.getString("country_code")?.let { country_code_string ->
            val resultArray = country_code_string.split("||")
            countryCode = resultArray[0]
            countryName = resultArray[1]
        }

        // 1.2. ViewModel. We bind our viewModel to the lifecycle of our activity, not fragment. We pass our activity as an owner of the lifecycle.
        // So, we need to do this way:
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        textRadioListTitle = view.findViewById(R.id.text_myFavorites_title)
        textRadioListTitle.text = countryName

        textRadioListSecondTitleSelect = view.findViewById(R.id.text_radioStationDialogTitleSelect)
        textRadioListSecondTitleSelect.isVisible = true
        val textLoading = activity?.getString(R.string.radioStationList_title_loading)
        textRadioListSecondTitleSelect.text = textLoading

        textRadioListSecondTitleDownload =
            view.findViewById(R.id.text_radioStationDialogTitleDownload)

        imageArrowBack = view.findViewById(R.id.image_arrowBack)
        textRadioStationsEmpty = view.findViewById(R.id.text_radioStationsEmpty)
        recyclerViewRadioStationList = view.findViewById(R.id.recyclerView_radioStationList)
        frameLayout = view.findViewById(R.id.frameLayout)
        progressCircular = view.findViewById(R.id.progressCircular)

        // Получаем список радиостанций, преобразуем. Сохранять в Room не будем. Радиостанций очень много, будет занимать много места на телефоне.
        // Кроме того, списки на сервере постоянно обновляются. Возможно какой-то радиостанции в списке уже не будет, а в локальной БД она ещё осталась. Пользователь выберет её и будет ошибка.
        viewModel.getRadioStationList(countryCode)

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

        // Инициализируем адаптер
        listRadioStationAdapter = ListRadioStationAdapter()

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

        changeTextSelectOrDownload()
    }

    private fun initListeners() {
        imageArrowBack.setOnClickListener {
            if (this.findNavController().currentDestination?.id == R.id.radioListFragment) {
                this.findNavController()
                    .navigate(R.id.action_radioListFragment_to_homeRadioFragment)
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
            // Возвращаем текст диалогового окна (на случай, если мы делали какие-то изменения во время пользования этим фрагментом)
            val titleInternetTrouble = getString(R.string.dialogInternetTrouble_title)
            val textInternetTrouble = getString(R.string.dialogInternetTrouble_text)
            val titleViewInternetTrouble =
                dialogInternetTrouble.findViewById<AppCompatTextView>(R.id.title_internetTrouble)
            val textViewInternetTrouble =
                dialogInternetTrouble.findViewById<AppCompatTextView>(R.id.text_internetTrouble)
            titleViewInternetTrouble.text = titleInternetTrouble
            textViewInternetTrouble.text = textInternetTrouble

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
        viewModel.radioStationListLiveData.observe(viewLifecycleOwner) { radioStationPresentationList ->
            radioStationList = radioStationPresentationList
            showProgress()

            changeTextSelectOrDownload()

            // 1.5. ОБРАБОТКА КЛИКА -> Получаем результат клика во фрагменте (описываем нашу анонимную функцию из RecyclerView)
//                recyclerViewRadioStationList.adapter =
//                    RadioListAdapter(radioStationList) { radioStationPresentationOnClick ->
//                        Log.d(TAG, "Выбранный элемент списка: $radioStationPresentationOnClick")
//
//                        // Открываем по клику другой фрагмент, передаём туда нашу радиостанцию
//                        val direction = RadioListFragmentDirections.actionRadioListFragmentToHomeRadioFragment(
//                                radioStationPresentationOnClick
//                            )
//                        this.findNavController().navigate(direction)
//                    }

            if (!radioStationPresentationList.isNullOrEmpty()) {

                textRadioStationsEmpty.isVisible = false

//                // Инициализируем адаптер
//                val listRadioStationAdapter = ListRadioStationAdapter()

                // Перезаписываем список радиостанций для адаптера
                listRadioStationAdapter.radioStationList = radioStationPresentationList
                // Определяем адаптер для recycler view
                recyclerViewRadioStationList.adapter =
                    listRadioStationAdapter
                // Устанавливаем Click Listener
                listRadioStationAdapter.setItemClickListener { radioStationPresentationOnClick ->
                    Log.d(TAG, "Выбранный элемент списка: $radioStationPresentationOnClick")

                    // Открываем по клику другой фрагмент, передаём туда нашу радиостанцию
                    val direction =
                        RadioListFragmentDirections.actionRadioListFragmentToHomeRadioFragment(
                            radioStationPresentationOnClick
                        )
                    if (this.findNavController().currentDestination?.id == R.id.radioListFragment) {
                        this.findNavController().navigate(direction)
                    }
                }

                textRadioListSecondTitleDownload.setOnClickListener {
                    Log.d(TAG, "Загружаем плейлист")

                    // Иногда после скачивания нового плейлиста экзоплейер не обновляется. Поэтому перед тем, как включить первую станцию нового плейлиста, укажем явно, что его нужно скачать
                    mainViewModel.fetchSongs(radioStationPresentationList[0].countryCode)

                    // Открываем по клику другой фрагмент, передаём туда нашу радиостанцию
                    val direction =
                        RadioListFragmentDirections.actionRadioListFragmentToHomeRadioFragment(
                            radioStationPresentationList[0]
                        )
                    if (this.findNavController().currentDestination?.id == R.id.radioListFragment) {
                        this.findNavController().navigate(direction)
                    }
                }

            } else {
                textRadioStationsEmpty.isVisible = true
                textRadioListSecondTitleSelect.isVisible = false
                hideProgress()
                return@observe
            }

            Log.d(
                TAG,
                "Успешный запрос в локальную БД (радиостанции). Получен результат: массив size = ${radioStationPresentationList.size}, элемент[0] = ${radioStationPresentationList[0].countryCode}, ${radioStationPresentationList[0].urlResolved}"
            )

            hideProgress()
        }
    }

    private fun changeTextSelectOrDownload() {
        val radioStationListFromFragment = viewModel.radioStationListLiveData.value

        // Список может оказаться пустым. Проверяем
        if (!radioStationListFromFragment.isNullOrEmpty()) {

            // Если он не пуст, проверяем его country code
            val radioCountryCodeFromFragment = radioStationListFromFragment[0].countryCode

            // И сравниваем с countrycode в плейере
            if (radioCountryCodeFromFragment == radioCountryCodeFromActivity) {
                textRadioListSecondTitleSelect.isVisible = true
                val textSelect = activity?.getString(R.string.radioStationList_title_select)
                textRadioListSecondTitleSelect.text = textSelect

                textRadioListSecondTitleDownload.isVisible = false
                listRadioStationAdapter.isClickableRecyclerView = true
            } else {
                textRadioListSecondTitleSelect.isVisible = false
                textRadioListSecondTitleDownload.isVisible = true
                listRadioStationAdapter.isClickableRecyclerView = false
            }

        } else {
            // List is empty
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