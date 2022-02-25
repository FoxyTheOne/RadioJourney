package com.myproject.radiojourney.data.dataSource.local

import androidx.lifecycle.MutableLiveData
import com.myproject.radiojourney.data.localDatabaseRoom.IUserDAO
import com.myproject.radiojourney.data.sharedPreference.IAppSharedPreference
import javax.inject.Inject

/**
 * LocalAuthDataSource Будет доставать данные, либо сохранять их в локальную базу данных (SharedPreference, Room)
 */
class LocalAuthDataSource @Inject constructor(
    private val preference: IAppSharedPreference,
    private val userDAO: IUserDAO
) : ILocalAuthDataSource {

    override fun getEmailIfCheckBoxSelected(): MutableLiveData<String> {
        val emailLiveData = MutableLiveData<String>()
        preference.let {
            if (it.isRememberLoginAndPasswordSelected()) {
                emailLiveData.postValue(it.getEmail()) // Т.е. вытягиваем сохраненные значения из local storage и кладем их в LiveData
            }
        }
        return emailLiveData
    }

    override fun getPasswordIfCheckBoxSelected(): MutableLiveData<String> {
        val passwordLiveData = MutableLiveData<String>()
        preference.let {
            if (it.isRememberLoginAndPasswordSelected()) {
                passwordLiveData.postValue(it.getPassword()) // Т.е. вытягиваем сохраненные значения из local storage и кладем их в LiveData
            }
        }
        return passwordLiveData
    }

    override fun isCheckBoxSelected(): MutableLiveData<Boolean> {
        val checkBoxRememberLoginAndPasswordLiveData = MutableLiveData<Boolean>()
        preference.let {
            if (it.isRememberLoginAndPasswordSelected()) {
                checkBoxRememberLoginAndPasswordLiveData.postValue(true) // кладем true
            }
        }
        return checkBoxRememberLoginAndPasswordLiveData
    }

    /**
     * Проверка, есть ли такой зарегистрированный User и верно ли введен пароль
     */
    override suspend fun onLoginClicked(emailText: String, passwordText: String): Boolean {
        val user = userDAO.getUser(emailText)
        val userPassword = user?.password
        val isPasswordTheSame = userPassword == passwordText

        return if (isPasswordTheSame) {
            preference.saveToken(user?.id ?: 0)
            true
        } else {
            false
        }
    }

    /**
     * Каждый раз, когда мы кликаем, будет исполняться этот метод. Здесь мы сохраняем статус check box
     */
    override suspend fun setRememberLoginAndPasswordSelectedOrNot(isSelected: Boolean) {
        preference.setRememberLoginAndPasswordSelectedOrNot(isSelected)
    }

}