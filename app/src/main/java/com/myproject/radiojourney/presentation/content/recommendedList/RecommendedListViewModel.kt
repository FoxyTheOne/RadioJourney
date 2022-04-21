package com.myproject.radiojourney.presentation.content.recommendedList

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.domain.homeRadio.IHomeRadioInteractor
import com.myproject.radiojourney.domain.logOut.ILogOutInteractor
import com.myproject.radiojourney.domain.recommendedList.IRecommendedListInteractor
import com.myproject.radiojourney.model.presentation.RadioStationPresentation
import com.myproject.radiojourney.utils.extension.call
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * ViewModel. Здесь осуществляется подписка, запрос через корутины. Работает с Interactor
 */
@HiltViewModel
class RecommendedListViewModel @Inject constructor(
    private val logOutInteractor: ILogOutInteractor,
    private val recommendedListInteractor: IRecommendedListInteractor
) : ViewModel() {
    companion object {
        private const val TAG = "RecommendedListViewModel"
    }

    val radioStationRecommendedListLiveData = MutableLiveData<List<RadioStationPresentation>>()
    val failedLiveData = MutableLiveData<Boolean>()
    val favoritesFailedLiveData = MutableLiveData<Boolean>()

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
            } catch (e: IOException) {
                e.printStackTrace()
                failedLiveData.call()
            }
        }
    }
}