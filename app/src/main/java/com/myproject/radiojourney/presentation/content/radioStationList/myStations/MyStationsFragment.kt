package com.myproject.radiojourney.presentation.content.radioStationList.myStations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.myproject.radiojourney.R
import com.myproject.radiojourney.databinding.LayoutMyStationsBinding
import com.myproject.radiojourney.presentation.common.collectWhenStarted
import com.myproject.radiojourney.presentation.content.base.BaseContentFragmentAbstract
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
class MyStationsFragment : BaseContentFragmentAbstract() {

    private val viewModel by viewModels<MyStationsViewModel>()

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: LayoutMyStationsBinding? = null

    private val myStationAdapter = MyStationAdapter(
        onStationClicked = { station -> openHomeRadioAndPlay(station) },
        onDeleteClicked = { station -> confirmDelete(station) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // VIEW BINDING -> 2. Инициализация
        binding = LayoutMyStationsBinding.inflate(inflater, container, false)
        // TOOLBAR - где будет находиться в нашем layout
        binding?.let { setToolbar(it.homeToolbar) }
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbarMenu()

        binding?.recyclerViewMyStations?.adapter = myStationAdapter

        view.findViewById<AppCompatImageView>(R.id.image_arrowBack)
            .setOnClickListener { goToHomeRadio() }

        view.findViewById<FloatingActionButton>(R.id.fab_addMyStation).setOnClickListener {
            AddMyStationDialogFragment().show(parentFragmentManager, null)
        }

        // Станция добавлена в окне "Новая станция" - подтверждаем это пользователю.
        // Сам список обновится сам: он подписан на базу данных
        parentFragmentManager.setFragmentResultListener(
            AddMyStationDialogFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, _ ->
            showMessage(R.string.myStations_added)
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
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.myStations_delete_title)
            .setMessage(getString(R.string.myStations_delete_text, station.stationName))
            .setPositiveButton(R.string.myStations_delete_confirm) { _, _ ->
                viewModel.deleteMyStation(station.stationuuid)
                showMessage(R.string.myStations_deleted)
            }
            .setNegativeButton(R.string.myStations_delete_cancel, null)
            .show()
    }

    // Включаем станцию: возвращаемся на главный экран и передаём её туда аргументом - так же,
    // как это делают список станций страны и список избранного
    private fun openHomeRadioAndPlay(station: RadioStationPresentation) {
        if (findNavController().currentDestination?.id == R.id.myStationsFragment) {
            findNavController().navigate(
                MyStationsFragmentDirections.actionMyStationsFragmentToHomeRadioFragment(station)
            )
        }
    }

    private fun goToHomeRadio() {
        if (findNavController().currentDestination?.id == R.id.myStationsFragment) {
            findNavController().navigate(R.id.action_myStationsFragment_to_homeRadioFragment)
        }
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