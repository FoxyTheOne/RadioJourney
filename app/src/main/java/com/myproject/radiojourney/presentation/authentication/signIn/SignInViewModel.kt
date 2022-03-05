package com.myproject.radiojourney.presentation.authentication.signIn

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.signIn.ISignInInteractor
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
) : ViewModel() {
    companion object {
        private const val TAG = "SignInViewModel"
    }

    var emailLiveData = MutableLiveData<String>()
    var passwordLiveData = MutableLiveData<String>()
    var checkBoxRememberLoginAndPasswordLiveData = MutableLiveData<Boolean>()

    // Успешная аутентификация
    val signInSuccessLiveData = MutableLiveData<Boolean>()

    // Ошибки аутентификации
    val showCredentialsErrorLiveData = MutableLiveData<Boolean>()

    // LiveData, которые будут отвечать за отображение прогресса (кружок)
    val showProgressLiveData = MutableLiveData<Boolean>()
    val hideProgressLiveData = MutableLiveData<Boolean>()

//    // Если в предыдущий раз галочка была выбрана - восстанавливаем сохраненные значения
//    fun getStoredData() {
//        // Dispatchers.IO создаёт необходмое количество потоков, но минимум 64. Предназначен для выполнения операций ввода-вывода (н-р, операции с файлами, сетевыми запросами, локальной базой данных)
//        // launch - потому что нам нужно просто сделать вызов функции
//        viewModelScope.launch(Dispatchers.IO) {
//            emailLiveData = signInInteractor.getEmailIfCheckBoxSelected()
//            passwordLiveData = signInInteractor.getPasswordIfCheckBoxSelected()
//            checkBoxRememberLoginAndPasswordLiveData = signInInteractor.isCheckBoxSelected()
//        }
//    }

    // Если в предыдущий раз галочка была выбрана - восстанавливаем сохраненные значения
    fun getStoredData() {
        viewModelScope.launch(Dispatchers.IO) {
            // Узнём, была ли выбрана галочка в последний раз
            val isRememberLoginAndPasswordSelected = signInInteractor.isRememberLoginAndPasswordSelected()
            Log.d(TAG, "Узнали, была ли выбрана галочка: CHECK_BOX_SELECTED = $isRememberLoginAndPasswordSelected")
            // Если да - восстанавливаем значения полей и ставим флажок в CheckBox
            if (isRememberLoginAndPasswordSelected) {
                val email = signInInteractor.getEmail().toString()
                val password = signInInteractor.getPassword().toString()
                Log.d(TAG, "Передаются значения в LiveData: email = $email, password = $password")
                emailLiveData.postValue(email)
                passwordLiveData.postValue(password)
                // Т.е. вытягиваем сохраненные значения из local storage и кладем их в LiveData
                checkBoxRememberLoginAndPasswordLiveData.postValue(true) // кладем true
            }
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
            Log.d(TAG, "Передано из ViewModel в signInInteractor: CHECK_BOX_SELECTED = $isSelected")
        }
    }
}