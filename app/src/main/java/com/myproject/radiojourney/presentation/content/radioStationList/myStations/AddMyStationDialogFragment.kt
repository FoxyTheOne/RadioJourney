package com.myproject.radiojourney.presentation.content.radioStationList.myStations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.myproject.radiojourney.R
import com.myproject.radiojourney.domain.myStationsUseCase.IMyStationsUseCase
import com.myproject.radiojourney.presentation.common.collectWhenStarted
import com.myproject.radiojourney.presentation.content.radioStationList.myStations.AddMyStationDialogFragment.Companion.ARG_STATION_UUID
import dagger.hilt.android.AndroidEntryPoint

/**
 * Окно "Новая станция" и оно же "Изменить станцию": название и ссылка на поток.
 *
 * Два режима в одном диалоге, потому что поля и проверки одинаковые - разными получаются только заголовок
 * и то, что происходит по кнопке "Сохранить". Если передан [ARG_STATION_UUID], это правка.
 *
 * Диалог закрывается только когда станция сохранена. Если название пустое или ссылка неверная,
 * он остаётся открытым и показывает ошибку под нужным полем - пользователю не приходится набирать всё заново.
 *
 * Об успешном сохранении экран списка узнаёт через Fragment Result API (как и диалог выхода):
 * так связь переживает пересоздание экрана при повороте
 */
@AndroidEntryPoint
class AddMyStationDialogFragment : DialogFragment() {

    companion object {
        // Ключ ответа "станция сохранена" (см. MyStationsFragment)
        const val REQUEST_KEY = "AddMyStationDialogFragment.stationSaved"

        // В ответе сообщаем, была это правка или добавление: от этого зависит текст подтверждения
        const val RESULT_IS_EDIT = "isEdit"

        private const val ARG_STATION_UUID = "stationUuid"
        private const val ARG_STATION_NAME = "stationName"
        private const val ARG_STATION_URL = "stationUrl"

        // Добавление новой станции
        fun newInstance() = AddMyStationDialogFragment()

        // Правка уже добавленной станции: поля сразу заполнены её значениями
        fun newInstanceForEdit(stationUuid: String, name: String, url: String) =
            AddMyStationDialogFragment().apply {
                arguments = bundleOf(
                    ARG_STATION_UUID to stationUuid,
                    ARG_STATION_NAME to name,
                    ARG_STATION_URL to url
                )
            }
    }

    private val viewModel by viewModels<AddMyStationViewModel>()

    // null - добавляем новую станцию, иначе редактируем эту
    private val editedStationUuid: String? get() = arguments?.getString(ARG_STATION_UUID)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Оформление окна (фон со скруглением, цвета текста и полей) берётся из темы приложения,
        // а не из разметки - тогда оно не зависит от того, светлая или тёмная тема стоит на телефоне
        setStyle(STYLE_NORMAL, R.style.ThemeOverlay_RadioJourney_CustomDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.layout_add_my_station_dialog, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title: AppCompatTextView = view.findViewById(R.id.title_addMyStation)
        val inputLayoutName: TextInputLayout = view.findViewById(R.id.inputLayout_myStationName)
        val inputLayoutUrl: TextInputLayout = view.findViewById(R.id.inputLayout_myStationUrl)
        val editName: TextInputEditText = view.findViewById(R.id.edit_myStationName)
        val editUrl: TextInputEditText = view.findViewById(R.id.edit_myStationUrl)
        val buttonCancel: AppCompatButton = view.findViewById(R.id.button_addMyStationCancel)
        val buttonSave: AppCompatButton = view.findViewById(R.id.button_addMyStationSave)

        val stationUuid = editedStationUuid
        title.setText(if (stationUuid == null) R.string.addMyStation_title else R.string.addMyStation_titleEdit)

        // Значения подставляем только при первом создании окна: после поворота экрана поля восстановит система,
        // и то, что пользователь успел набрать, не должно замениться исходным названием
        if (savedInstanceState == null && stationUuid != null) {
            editName.setText(arguments?.getString(ARG_STATION_NAME))
            editUrl.setText(arguments?.getString(ARG_STATION_URL))
        }

        buttonCancel.setOnClickListener { dismiss() }

        buttonSave.setOnClickListener {
            // Старые ошибки убираем: иначе они остаются висеть, пока пользователь исправляет поле
            inputLayoutName.error = null
            inputLayoutUrl.error = null

            val name = editName.text.toString()
            val url = editUrl.text.toString()
            if (stationUuid == null) {
                viewModel.addMyStation(name, url)
            } else {
                viewModel.editMyStation(stationUuid, name, url)
            }
        }

        viewLifecycleOwner.collectWhenStarted(viewModel.addResults) { result ->
            when (result) {
                IMyStationsUseCase.AddResult.ADDED -> {
                    // Сообщаем экрану списка, что станция сохранена (он покажет Snackbar), и закрываемся
                    parentFragmentManager.setFragmentResult(
                        REQUEST_KEY,
                        bundleOf(RESULT_IS_EDIT to (stationUuid != null))
                    )
                    dismiss()
                }

                IMyStationsUseCase.AddResult.EMPTY_NAME ->
                    inputLayoutName.error = getString(R.string.addMyStation_error_name)

                IMyStationsUseCase.AddResult.INVALID_URL ->
                    inputLayoutUrl.error = getString(R.string.addMyStation_error_url)

                IMyStationsUseCase.AddResult.DUPLICATE_URL ->
                    inputLayoutUrl.error = getString(R.string.addMyStation_error_duplicate)
            }
        }
    }
}