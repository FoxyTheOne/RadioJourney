package com.myproject.radiojourney.presentation.content.recommendedList

import android.accounts.AccountsException
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.logOut.ILogOutUseCase
import com.myproject.radiojourney.domain.recommendedList.IRecommendedListUseCase
import com.myproject.radiojourney.model.presentation.RadioStationPresentation
import com.myproject.radiojourney.utils.extension.call
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * Presentation layer, ViewModel. Работа с компонентами Android. Работает только с Interactor.
 *
 * Interactor - объект, который реализует UseCase, используя бизнес-объекты Entities.
 * Здесь осуществляется подписка, запрос через корутины.
 */
@HiltViewModel
class RecommendedListViewModel @Inject constructor(
    private val logOutInteractor: ILogOutUseCase,
    private val recommendedListInteractor: IRecommendedListUseCase
) : ViewModel() {

    val radioStationRecommendedListLiveData = MutableLiveData<List<RadioStationPresentation>>()
    val failedLiveData = MutableLiveData<Boolean>()

    // LiveData для открытия диалогового окна
    val dialogInternetTroubleLiveData = MutableLiveData<Boolean>()

    // LiveData, которые будут отвечать за отображение прогресса (кружок)
    val showProgressLiveData = MutableLiveData<Boolean>()
    val hideProgressLiveData = MutableLiveData<Boolean>()

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            showProgressLiveData.call()
            logOutInteractor.onLogout()
            hideProgressLiveData.call()
        }
    }

    fun getRadioStationRecommendedListAndShow() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val radioStationRecommendedList =
                    recommendedListInteractor.getRadioStationRecommendedList()
                radioStationRecommendedListLiveData.postValue(
                    radioStationRecommendedList
                )
            } catch (e1: AccountsException) {
                e1.printStackTrace()
                dialogInternetTroubleLiveData.call()
            } catch (e: IOException) {
                e.printStackTrace()
                failedLiveData.call()
            }
        }
    }

}