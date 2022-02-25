package com.myproject.radiojourney.presentation.authentication.signIn

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.myproject.radiojourney.R
import com.myproject.radiojourney.databinding.LayoutSignInBinding

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignInFragment: Fragment() {
    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: LayoutSignInBinding? = null
    private val viewModel by viewModels<SignInViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = LayoutSignInBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Если в предыдущий раз галочка была выбрана - восстанавливаем сохраненные значения
        viewModel.getStoredData()

        initListeners()
        subscribeOnLiveData()
    }

    private fun initListeners() {
        // Сохраняем введенные в поля значения для последующего восстановления при необходимости:
        binding?.textFieldEmail?.editText?.addTextChangedListener {
            it?.let {
                viewModel.setUpdatedEmail(it.toString())

                // Изменение цвета границы поля ввода при вводе символов в это поле:
                if (it.isBlank()) {
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.box_stroke_color_default
                    )?.let { colorList ->
                        binding?.textFieldEmail?.setBoxStrokeColorStateList(colorList)
                    }
                } else {
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.box_stroke_color_with_text
                    )?.let { colorList ->
                        binding?.textFieldEmail?.setBoxStrokeColorStateList(colorList)
                        binding?.textFieldEmail?.hintTextColor = colorList
                    }
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.icon_in_box_with_text
                    )?.let { colorList ->
                        binding?.textFieldEmail?.setStartIconTintList(colorList)
                    }
                }

            }
        }

        binding?.textFieldPassword?.editText?.addTextChangedListener {
            it?.let {
                viewModel.setUpdatedPassword(it.toString())

                // Изменение цвета границы поля ввода при вводе символов в это поле:
                if (it.isBlank()) {
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.box_stroke_color_default
                    )?.let { colorList ->
                        binding?.textFieldPassword?.setBoxStrokeColorStateList(colorList)
                    }
                } else {
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.box_stroke_color_with_text
                    )?.let { colorList ->
                        binding?.textFieldPassword?.setBoxStrokeColorStateList(colorList)
                        binding?.textFieldPassword?.hintTextColor = colorList
                    }
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.icon_in_box_with_text
                    )?.let { colorList ->
                        binding?.textFieldPassword?.setStartIconTintList(colorList)
                        binding?.textFieldPassword?.setEndIconTintList(colorList)
                    }
                }

            }
        }

        // Определяем действие по клику на кнопку:
        binding?.buttonLogin?.setOnClickListener {
            val emailText = binding?.textFieldEmail?.editText?.text.toString()
            val passwordText = binding?.textFieldPassword?.editText?.text.toString()

            viewModel.onLoginClicked(emailText, passwordText)
        }

        // Переход на signUpFragment
        binding?.linearBottomComponentTextGoToSignUp?.setOnClickListener {
            this.findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
        }

        // Каждый раз, когда мы кликаем, будет исполняться этот метод. Здесь мы сохраняем статус check box
        binding?.checkBoxRememberLoginAndPassword?.setOnCheckedChangeListener{ _, selected ->
            viewModel.setRememberLoginAndPasswordSelectedOrNot(selected) // Передаём наш isSelected (при нажатии на кнопку) в наш listener
        }
    }

    private fun subscribeOnLiveData() {
        // Переход на контент в случае успешной аутентификации
        viewModel.signInSuccessLiveData.observe(viewLifecycleOwner, {
            this.findNavController().navigate(R.id.action_signInFragment_to_content_nav_graph)
        })

        viewModel.showCredentialsErrorLiveData.observe(viewLifecycleOwner, {
            binding?.textFieldEmail?.error = getString(R.string.signIn_credentials_incorrect)
            binding?.textFieldPassword?.error = getString(R.string.signIn_credentials_incorrect)
            Toast.makeText(context, "Something wrong with your data. Please, try again!", Toast.LENGTH_LONG).show()
        })

        // Показываем или ппрячем Progress
        viewModel.showProgressLiveData.observe(viewLifecycleOwner, {
            showProgress()
        })
        viewModel.hideProgressLiveData.observe(viewLifecycleOwner, {
            hideProgress()
        })

        // Слушаем check box
        viewModel.checkBoxRememberLoginAndPasswordLiveData.observe(viewLifecycleOwner, { isSelected ->
            binding?.checkBoxRememberLoginAndPassword?.isChecked = isSelected
        })

        // Слушаем email и password
        viewModel.emailLiveData.observe(viewLifecycleOwner, { email ->
            binding?.textFieldEmail?.editText?.setText(email)
            binding?.textFieldEmail?.editText?.setSelection(email.length)
        })
        viewModel.passwordLiveData.observe(viewLifecycleOwner, { password ->
            binding?.textFieldPassword?.editText?.setText(password)
            binding?.textFieldPassword?.editText?.setSelection(password.length)
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

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}