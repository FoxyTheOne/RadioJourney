package com.myproject.radiojourney.presentation.authentication.signUp

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
import com.myproject.radiojourney.databinding.LayoutSignUpBinding
import com.myproject.radiojourney.presentation.authentication.base.BaseAuthFragmentAbstract
import dagger.hilt.android.AndroidEntryPoint

/**
 * Аутентификация. Фрагмент для регистрации
 */
@AndroidEntryPoint
class SignUpFragment : BaseAuthFragmentAbstract() {
    companion object {
        private const val TAG = "SignUpFragment"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: LayoutSignUpBinding? = null
    private val viewModel by viewModels<SignUpViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = LayoutSignUpBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initListeners()
        subscribeOnLiveData()
    }

    private fun initListeners() {
        // Сохраняем введенные в поля значения для последнующего восстановления при необходимости:
        binding?.textFieldEmailSignUp?.editText?.addTextChangedListener() {
            it?.let {

                // Изменение цвета границы поля ввода при вводе символов в это поле:
                if (it.isBlank()) {
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.box_stroke_color_default
                    )?.let { colorList ->
                        binding?.textFieldEmailSignUp?.setBoxStrokeColorStateList(colorList)
                    }
                } else {
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.box_stroke_color_with_text
                    )?.let { colorList ->
                        binding?.textFieldEmailSignUp?.setBoxStrokeColorStateList(colorList)
                        binding?.textFieldEmailSignUp?.hintTextColor = colorList
                    }
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.icon_in_box_with_text
                    )?.let { colorList ->
                        binding?.textFieldEmailSignUp?.setStartIconTintList(colorList)
                    }
                }

            }
        }

        binding?.textFieldPasswordSignUp?.editText?.addTextChangedListener() {
            it?.let {

                // Изменение цвета границы поля ввода при вводе символов в это поле:
                if (it.isBlank()) {
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.box_stroke_color_default
                    )?.let { colorList ->
                        binding?.textFieldPasswordSignUp?.setBoxStrokeColorStateList(colorList)
                    }
                } else {
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.box_stroke_color_with_text
                    )?.let { colorList ->
                        binding?.textFieldPasswordSignUp?.setBoxStrokeColorStateList(colorList)
                        binding?.textFieldPasswordSignUp?.hintTextColor = colorList
                    }
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.icon_in_box_with_text
                    )?.let { colorList ->
                        binding?.textFieldPasswordSignUp?.setStartIconTintList(colorList)
                    }
                }

            }
        }

        binding?.textFieldConfirmPasswordSignUp?.editText?.addTextChangedListener() {
            it?.let {

                // Изменение цвета границы поля ввода при вводе символов в это поле:
                if (it.isBlank()) {
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.box_stroke_color_default
                    )?.let { colorList ->
                        binding?.textFieldConfirmPasswordSignUp?.setBoxStrokeColorStateList(
                            colorList
                        )
                    }
                } else {
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.box_stroke_color_with_text
                    )?.let { colorList ->
                        binding?.textFieldConfirmPasswordSignUp?.setBoxStrokeColorStateList(
                            colorList
                        )
                        binding?.textFieldConfirmPasswordSignUp?.hintTextColor = colorList
                    }
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.icon_in_box_with_text
                    )?.let { colorList ->
                        binding?.textFieldConfirmPasswordSignUp?.setStartIconTintList(colorList)
                        binding?.textFieldConfirmPasswordSignUp?.setEndIconTintList(colorList)
                    }
                }

            }
        }

        // Определяем действие по клику на кнопку:
        binding?.buttonSignUp?.setOnClickListener {
            val emailText = binding?.textFieldEmailSignUp?.editText?.text.toString()
            val passwordText = binding?.textFieldPasswordSignUp?.editText?.text.toString()
            val confirmPasswordText =
                binding?.textFieldConfirmPasswordSignUp?.editText?.text.toString()

            viewModel.onSignUpClicked(emailText, passwordText, confirmPasswordText)
        }

        // Переход на SignInFragment
        binding?.linearSignUnBottomComponentTextGoToSignIn?.setOnClickListener {
            this.findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
        }
    }

    private fun subscribeOnLiveData() {
        // Переход на контент в случае успешной аутентификации
        viewModel.signUpSuccessLiveData.observe(viewLifecycleOwner, { email ->
            // Перенесём email нового зарегистрированного пользователя на SignIn (передадим аргумент)
            val direction = SignUpFragmentDirections.actionSignUpFragmentToSignInFragment(email)
            this.findNavController().navigate(direction)
            Toast.makeText(
                context,
                "Successful registration. Now log in and begin your radio journey!",
                Toast.LENGTH_LONG
            ).show()
            Log.d(TAG, "signUpSuccessLiveData called")
        })

        // Ошибки аутентификации
        viewModel.showEmailErrorLiveData.observe(this, {
            binding?.textFieldEmailSignUp?.error = getString(R.string.signUp_email_incorrect)
            Toast.makeText(
                context,
                "Something wrong with your email. Please, try again!",
                Toast.LENGTH_LONG
            ).show()
            Log.d(TAG, "showEmailErrorLiveData called")
        })
        viewModel.showPasswordErrorLiveData.observe(this, {
            binding?.textFieldPasswordSignUp?.error = getString(R.string.signUp_password_incorrect)
            binding?.textFieldConfirmPasswordSignUp?.error =
                getString(R.string.signUp_password_incorrect)
            Toast.makeText(
                context,
                "Something wrong with your password. Please, try again!",
                Toast.LENGTH_LONG
            ).show()
            Log.d(TAG, "showPasswordErrorLiveData called")
        })
        viewModel.emptyEmailErrorLiveData.observe(this, {
            binding?.textFieldEmailSignUp?.error = null
            Log.d(TAG, "emptyEmailErrorLiveData called")
        })
        viewModel.emptyPasswordErrorLiveData.observe(this, {
            binding?.textFieldPasswordSignUp?.error = null
            binding?.textFieldConfirmPasswordSignUp?.error = null
            Log.d(TAG, "emptyPasswordErrorLiveData called")
        })

        // Показываем или прячем Progress
        viewModel.showProgressLiveData.observe(viewLifecycleOwner, {
            showProgress()
        })
        viewModel.hideProgressLiveData.observe(viewLifecycleOwner, {
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

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}