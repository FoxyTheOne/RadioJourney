package com.myproject.radiojourney.presentation.content.radioStationList.myStations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.myproject.radiojourney.R
import com.myproject.radiojourney.databinding.LayoutMyStationsBinding
import com.myproject.radiojourney.other.Constants.MY_STATIONS_COUNTRY_CODE
import com.myproject.radiojourney.presentation.MainViewModel
import com.myproject.radiojourney.presentation.common.collectWhenStarted
import com.myproject.radiojourney.presentation.common.navigateSafely
import com.myproject.radiojourney.presentation.common.popBackStackSafely
import com.myproject.radiojourney.presentation.model.RadioStationPresentation
import dagger.hilt.android.AndroidEntryPoint

/**
 * Экран "Мои радиостанции": станции, которые пользователь добавил сам, по ссылке на поток.
 *
 * Чем отличается от избранного: в избранное попадают станции из каталога radio-browser (звёздочкой),
 * а здесь пользователь заводит свои - например, местное радио, которого в каталоге нет.
 *
 * Клик по станции включает её так же, как и станцию из каталога: плееру всё равно, откуда пришёл адрес потока
 */
@AndroidEntryPoint
class MyStationsFragment : Fragment() {

    private val viewModel by viewModels<MyStationsViewModel>()

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: LayoutMyStationsBinding? = null

    // ViewModel плеера привязана к Activity: после правки или удаления станции плейлист в плеере нужно перечитать
    private val mainViewModel by activityViewModels<MainViewModel>()

    private val myStationAdapter = MyStationAdapter(
        onStationClicked = { station -> openHomeRadioAndPlay(station) },
        onEditClicked = { station ->
            AddMyStationDialogFragment
                .newInstanceForEdit(station.stationuuid, station.stationName, station.urlResolved)
                .show(parentFragmentManager, null)
        },
        onDeleteClicked = { station -> confirmDelete(station) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // VIEW BINDING -> 2. Инициализация
        binding = LayoutMyStationsBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.recyclerViewMyStations?.adapter = myStationAdapter

        view.findViewById<AppCompatImageView>(R.id.image_arrowBack)
            .setOnClickListener { goToHomeRadio() }

        view.findViewById<FloatingActionButton>(R.id.fab_addMyStation).setOnClickListener {
            AddMyStationDialogFragment.newInstance().show(parentFragmentManager, null)
        }

        // Станция сохранена в окне "Новая станция" / "Изменить станцию" - подтверждаем это пользователю.
        // Сам список обновится сам: он подписан на базу данных
        parentFragmentManager.setFragmentResultListener(
            AddMyStationDialogFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, result ->
            val isEdit = result.getBoolean(AddMyStationDialogFragment.RESULT_IS_EDIT)
            showMessage(if (isEdit) R.string.myStations_saved else R.string.myStations_added)
            reloadPlayerPlaylistIfMyStations()
        }

        subscribeOnFlow()
    }

    private fun subscribeOnFlow() {
        viewLifecycleOwner.collectWhenStarted(viewModel.myStations) { stations ->
            if (stations == null) return@collectWhenStarted // список ещё читается из базы

            myStationAdapter.myStationList = stations
            binding?.textMyStationsEmpty?.isVisible = stations.isEmpty()
        }
    }

    // Удаление - действие необратимое, поэтому переспрашиваем. В тексте показываем название станции,
    // чтобы было видно, какую именно удаляем (кнопок удаления в списке много)
    private fun confirmDelete(station: RadioStationPresentation) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.myStations_delete_title)
            .setMessage(getString(R.string.myStations_delete_text, station.stationName))
            .setPositiveButton(R.string.myStations_delete_confirm) { _, _ ->
                viewModel.deleteMyStation(station.stationuuid)
                showMessage(R.string.myStations_deleted)
                reloadPlayerPlaylistIfMyStations()
            }
            .setNegativeButton(R.string.myStations_delete_cancel, null)
            .show()
    }

    // Включаем станцию: возвращаемся на главный экран и передаём её туда аргументом - так же,
    // как это делают список станций страны и список избранного
    private fun openHomeRadioAndPlay(station: RadioStationPresentation) {
        navigateSafely(
            MyStationsFragmentDirections.actionMyStationsFragmentToHomeRadioFragment(
                station
            )
        )
    }

    // Назад на карту - так же, как системная кнопка "Назад" (см. комментарий в app_navigation.xml)
    private fun goToHomeRadio() {
        popBackStackSafely()
    }

    // Если в плеере сейчас плейлист своих станций, он держит их старые названия и ссылки - просим перечитать список.
    // Для других плейлистов делать нечего: свои станции в них не попадают
    private fun reloadPlayerPlaylistIfMyStations() {
        val isMyStationsInPlayer =
            mainViewModel.currentPlaylistStations.firstOrNull()?.countryCode == MY_STATIONS_COUNTRY_CODE
        if (isMyStationsInPlayer) mainViewModel.fetchSongs(MY_STATIONS_COUNTRY_CODE)
    }

    private fun showMessage(messageId: Int) {
        binding?.let { Snackbar.make(it.root, getString(messageId), Snackbar.LENGTH_SHORT).show() }
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        // Адаптер держит ссылку на view элементов списка - отвязываем его вместе с экраном
        binding?.recyclerViewMyStations?.adapter = null
        super.onDestroyView()
        binding = null
    }
}