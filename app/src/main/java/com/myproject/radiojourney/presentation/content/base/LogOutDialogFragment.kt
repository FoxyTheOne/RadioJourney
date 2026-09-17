package com.myproject.radiojourney.presentation.content.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.DialogFragment
import com.myproject.radiojourney.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Фрагмент всплывающего окна для уточнения перед выходом
 */
@AndroidEntryPoint
class LogOutDialogFragment : DialogFragment() {

    companion object {
        // Ключ ответа "пользователь подтвердил выход" (см. BaseContentFragmentAbstract)
        const val REQUEST_KEY = "LogOutDialogFragment.logOut"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_logout_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val buttonCancel: AppCompatButton = view.findViewById(R.id.button_logoutCancel)
        val buttonYes: AppCompatButton = view.findViewById(R.id.button_logoutYes)

        buttonCancel.setOnClickListener {
            dismiss()
        }
        buttonYes.setOnClickListener {
            // По кнопке "Да" отправляем ответ фрагменту, который открыл диалог (onLogOut() описан в content фрагменте с toolbar)
            parentFragmentManager.setFragmentResult(REQUEST_KEY, Bundle.EMPTY)
            dismiss()
        }
    }

}