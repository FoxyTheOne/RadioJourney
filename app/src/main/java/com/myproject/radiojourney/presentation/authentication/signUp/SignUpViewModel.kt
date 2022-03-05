package com.myproject.radiojourney.presentation.authentication.signUp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.signUp.ISignUpInteractor
import com.myproject.radiojourney.extension.call
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel. Здесь осуществляется подписка, запрос через корутины. Работает с Interactor
 */
@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpInteractor: ISignUpInteractor
) : ViewModel() {
    // Успешная аутентификация
    val signUpSuccessLiveData = MutableLiveData<String>()

    // Ошибки аутентификации
    val showEmailErrorLiveData = MutableLiveData<Boolean>()
    val showPasswordErrorLiveData = MutableLiveData<Boolean>()
    val emptyEmailErrorLiveData = MutableLiveData<Boolean>()
    val emptyPasswordErrorLiveData = MutableLiveData<Boolean>()

    // LiveData, которые будут отвечать за отображение прогресса (кружок)
    val showProgressLiveData = MutableLiveData<Boolean>()
    val hideProgressLiveData = MutableLiveData<Boolean>()

    fun onSignUpClicked(email: String, password: String, confirmPassword: String) {
        showProgressLiveData.call()
        // Показываем прогресс и делаем запрос в корутине
        viewModelScope.launch(Dispatchers.IO) {
            // Передаём в интерактор введенный текст для проверки
            val isEmailValid = signUpInteractor.checkEmail(email)
            val isPasswordsValid = signUpInteractor.checkPassword(password, confirmPassword)

            // Обработка ошибок
            if (!isEmailValid) {
                showEmailErrorLiveData.call()
                hideProgressLiveData.call()
                return@launch
            } else {
                emptyEmailErrorLiveData.call()
            }

            if (!isPasswordsValid) {
                showPasswordErrorLiveData.call()
                hideProgressLiveData.call()
                return@launch
            } else {
                emptyPasswordErrorLiveData.call()
            }

            // Если ошибок нет - успешная регистрация
            signUpInteractor.registerNewUser(email, password)
            hideProgressLiveData.call()
            signUpSuccessLiveData.postValue(email)
        }
    }
}