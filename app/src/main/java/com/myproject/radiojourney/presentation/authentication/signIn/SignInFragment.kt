package com.myproject.radiojourney.presentation.authentication.signIn

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.myproject.radiojourney.R
import com.myproject.radiojourney.databinding.LayoutSignInBinding
import com.myproject.radiojourney.presentation.authentication.base.BaseAuthFragmentAbstract

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignInFragment: BaseAuthFragmentAbstract() {
    companion object {
        private const val TAG = "SignUpFragment"
    }

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

        // Получаем результат при успешной регистрации на предыдущей странице
        arguments?.getString("user_email")?.let { email ->
            binding?.textFieldEmailSignIn?.editText?.setText(email)
        }
    }

    private fun initListeners() {
        // Сохраняем введенные в поля значения для последующего восстановления при необходимости:
        binding?.textFieldEmailSignIn?.editText?.addTextChangedListener {
            it?.let {
                viewModel.setUpdatedEmail(it.toString())

                // Изменение цвета границы поля ввода при вводе символов в это поле:
                if (it.isBlank()) {
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.box_stroke_color_default
                    )?.let { colorList ->
                        binding?.textFieldEmailSignIn?.setBoxStrokeColorStateList(colorList)
                    }
                } else {
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.box_stroke_color_with_text
                    )?.let { colorList ->
                        binding?.textFieldEmailSignIn?.setBoxStrokeColorStateList(colorList)
                        binding?.textFieldEmailSignIn?.hintTextColor = colorList
                    }
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.icon_in_box_with_text
                    )?.let { colorList ->
                        binding?.textFieldEmailSignIn?.setStartIconTintList(colorList)
                    }
                }

            }
        }

        binding?.textFieldPasswordSignIn?.editText?.addTextChangedListener {
            it?.let {
                viewModel.setUpdatedPassword(it.toString())

                // Изменение цвета границы поля ввода при вводе символов в это поле:
                if (it.isBlank()) {
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.box_stroke_color_default
                    )?.let { colorList ->
                        binding?.textFieldPasswordSignIn?.setBoxStrokeColorStateList(colorList)
                    }
                } else {
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.box_stroke_color_with_text
                    )?.let { colorList ->
                        binding?.textFieldPasswordSignIn?.setBoxStrokeColorStateList(colorList)
                        binding?.textFieldPasswordSignIn?.hintTextColor = colorList
                    }
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.icon_in_box_with_text
                    )?.let { colorList ->
                        binding?.textFieldPasswordSignIn?.setStartIconTintList(colorList)
                        binding?.textFieldPasswordSignIn?.setEndIconTintList(colorList)
                    }
                }

            }
        }

        // Определяем действие по клику на кнопку:
        binding?.buttonLogin?.setOnClickListener {
            val emailText = binding?.textFieldEmailSignIn?.editText?.text.toString()
            val passwordText = binding?.textFieldPasswordSignIn?.editText?.text.toString()

            viewModel.onLoginClicked(emailText, passwordText)
        }

        // Переход на signUpFragment
        binding?.linearSignInBottomComponentTextGoToSignUp?.setOnClickListener {
            this.findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
        }

        // Каждый раз, когда мы кликаем, будет исполняться этот метод. Здесь мы сохраняем статус check box
        binding?.checkBoxSignInRememberLoginAndPassword?.setOnCheckedChangeListener{ _, selected ->
            viewModel.setRememberLoginAndPasswordSelectedOrNot(selected) // Передаём наш isSelected (при нажатии на кнопку) в наш listener
            Log.d(TAG, "Проверка CHECK_BOX_SELECTED = $selected")
        }
    }

    private fun subscribeOnLiveData() {
        // Переход на контент в случае успешной аутентификации
        viewModel.signInSuccessLiveData.observe(viewLifecycleOwner, {
            this.findNavController().navigate(R.id.action_signInFragment_to_content_nav_graph)
        })

        viewModel.showCredentialsErrorLiveData.observe(viewLifecycleOwner, {
            binding?.textFieldEmailSignIn?.error = getString(R.string.signIn_credentials_incorrect)
            binding?.textFieldPasswordSignIn?.error = getString(R.string.signIn_credentials_incorrect)
            Toast.makeText(context, "Something wrong with your data. Please, try again!", Toast.LENGTH_LONG).show()
        })

        // Показываем или прячем Progress
        viewModel.showProgressLiveData.observe(viewLifecycleOwner, {
            showProgress()
        })
        viewModel.hideProgressLiveData.observe(viewLifecycleOwner, {
            hideProgress()
        })

        // Слушаем check box
        viewModel.checkBoxRememberLoginAndPasswordLiveData.observe(viewLifecycleOwner, { isSelected ->
            binding?.checkBoxSignInRememberLoginAndPassword?.isChecked = isSelected
        })

        // Слушаем email и password
        viewModel.emailLiveData.observe(viewLifecycleOwner, { email ->
            Log.d(TAG, "Восстановление текста email = $email")
            binding?.textFieldEmailSignIn?.editText?.setText(email)
            binding?.textFieldEmailSignIn?.editText?.setSelection(email.length)
        })
        viewModel.passwordLiveData.observe(viewLifecycleOwner, { password ->
            Log.d(TAG, "Восстановление текста password = $password")
            binding?.textFieldPasswordSignIn?.editText?.setText(password)
            binding?.textFieldPasswordSignIn?.editText?.setSelection(password.length)
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