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

    // Переменная нашего интерфейса, чтобы вызвать его метод
    private var listener: ILogOutListener? = null

    fun setLogOutListener(listener: ILogOutListener) {
        this.listener = listener
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
            // По кнопке да вызываем наш метод из интерфейса. Описываем его в фрагменте, в котором расположен тулбар (content фрагмент).
            // BaseContentFragmentAbstract должен наследоваться от этого интерфейса, чтобы в content фрагменте описать этот метод
            listener?.onLogOut()
            dismiss()
        }
    }

}