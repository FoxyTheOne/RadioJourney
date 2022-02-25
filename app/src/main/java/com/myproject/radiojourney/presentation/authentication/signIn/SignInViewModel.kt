package com.myproject.radiojourney.presentation.authentication.signIn

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.ISignInInteractor
import com.myproject.radiojourney.extension.call
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Подписка, запрос. Работает с Interactor
 */
@HiltViewModel
class SignInViewModel @Inject constructor(
    private var signInInteractor: ISignInInteractor
): ViewModel() {

    var emailLiveData = MutableLiveData<String>()
    var passwordLiveData = MutableLiveData<String>()
    var checkBoxRememberLoginAndPasswordLiveData = MutableLiveData<Boolean>()
    val signInSuccessLiveData = MutableLiveData<Boolean>()
    val showCredentialsErrorLiveData = MutableLiveData<Boolean>()
    // LiveData, которые будут отвечать за отображение прогресса (кружок)
    val showProgressLiveData = MutableLiveData<Boolean>()
    val hideProgressLiveData = MutableLiveData<Boolean>()

    // Если в предыдущий раз галочка была выбрана - восстанавливаем сохраненные значения
    fun getStoredData() {
        // Dispatchers.IO создаёт необходмое количество потоков, но минимум 64. Предназначен для выполнения операций ввода-вывода (н-р, операции с файлами, сетевыми запросами, локальной базой данных)
        // launch - потому что нам нужно просто сделать вызов функции
        viewModelScope.launch(Dispatchers.IO) {
            emailLiveData = signInInteractor.getEmailIfCheckBoxSelected()
            passwordLiveData = signInInteractor.getPasswordIfCheckBoxSelected()
            checkBoxRememberLoginAndPasswordLiveData = signInInteractor.isCheckBoxSelected()
        }
    }

    fun setUpdatedEmail(email: String) {
        if (email != emailLiveData.value) {
            emailLiveData.value = email
        }
    }

    fun setUpdatedPassword(password: String) {
        if (password != passwordLiveData.value) {
            passwordLiveData.value = password
        }
    }

    fun onLoginClicked(emailText: String, passwordText: String) {
        showProgressLiveData.call() // Сообщаем нашему view (LoginActivityView), что нужно показать прогресс

        viewModelScope.launch(Dispatchers.IO) {
            // Проверка, есть ли такой зарегистрированный User и верно ли введен пароль
            val isSignInSuccess = signInInteractor.onLoginClicked(emailText, passwordText)

            if (isSignInSuccess) {
                signInSuccessLiveData.call()
            } else {
                showCredentialsErrorLiveData.call()
            }
            hideProgressLiveData.call()
        }
    }

    // Каждый раз, когда мы кликаем, будет исполняться этот метод. Здесь мы сохраняем статус check box
    fun setRememberLoginAndPasswordSelectedOrNot(isSelected: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            signInInteractor.setRememberLoginAndPasswordSelectedOrNot(isSelected)
        }
    }

}