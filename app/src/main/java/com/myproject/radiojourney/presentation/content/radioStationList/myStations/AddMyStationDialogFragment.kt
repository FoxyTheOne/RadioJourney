package com.myproject.radiojourney.presentation.content.radioStationList.myStations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.myproject.radiojourney.R
import com.myproject.radiojourney.domain.myStationsUseCase.IMyStationsUseCase
import com.myproject.radiojourney.presentation.common.collectWhenStarted
import dagger.hilt.android.AndroidEntryPoint

/**
 * Окно "Новая станция": название и ссылка на поток.
 *
 * Диалог закрывается только когда станция сохранена. Если название пустое или ссылка неверная,
 * он остаётся открытым и показывает ошибку под нужным полем - пользователю не приходится набирать всё заново.
 *
 * Об успешном добавлении экран списка узнаёт через Fragment Result API (как и диалог выхода):
 * так связь переживает пересоздание экрана при повороте
 */
@AndroidEntryPoint
class AddMyStationDialogFragment : DialogFragment() {

    companion object {
        // Ключ ответа "станция добавлена" (см. MyStationsFragment)
        const val REQUEST_KEY = "AddMyStationDialogFragment.stationAdded"
    }

    private val viewModel by viewModels<AddMyStationViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.layout_add_my_station_dialog, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inputLayoutName: TextInputLayout = view.findViewById(R.id.inputLayout_myStationName)
        val inputLayoutUrl: TextInputLayout = view.findViewById(R.id.inputLayout_myStationUrl)
        val editName: TextInputEditText = view.findViewById(R.id.edit_myStationName)
        val editUrl: TextInputEditText = view.findViewById(R.id.edit_myStationUrl)
        val buttonCancel: AppCompatButton = view.findViewById(R.id.button_addMyStationCancel)
        val buttonSave: AppCompatButton = view.findViewById(R.id.button_addMyStationSave)

        buttonCancel.setOnClickListener { dismiss() }

        buttonSave.setOnClickListener {
            // Старые ошибки убираем: иначе они остаются висеть, пока пользователь исправляет поле
            inputLayoutName.error = null
            inputLayoutUrl.error = null
            viewModel.addMyStation(editName.text.toString(), editUrl.text.toString())
        }

        viewLifecycleOwner.collectWhenStarted(viewModel.addResults) { result ->
            when (result) {
                IMyStationsUseCase.AddResult.ADDED -> {
                    // Сообщаем экрану списка, что станция добавлена (он покажет Snackbar), и закрываемся
                    parentFragmentManager.setFragmentResult(REQUEST_KEY, Bundle.EMPTY)
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