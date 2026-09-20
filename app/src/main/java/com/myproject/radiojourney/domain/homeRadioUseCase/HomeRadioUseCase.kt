package com.myproject.radiojourney.domain.homeRadioUseCase

import com.myproject.radiojourney.domain.iRepository.IMainRadioStationRepository
import com.myproject.radiojourney.domain.model.Country
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin. Работает только с Repository.
 */
class HomeRadioUseCase @Inject constructor(
    private val mainRadioStationRepository: IMainRadioStationRepository
) : IHomeRadioUseCase {

    // Страны с радиостанциями из Room. Room сам выполняет запросы Flow в фоновом потоке
    override fun subscribeOnCountryList(): Flow<List<Country>> =
        mainRadioStationRepository.subscribeOnCountryList()

    override suspend fun setIsHideInfoClicked(isHideInfoClicked: Boolean) =
        mainRadioStationRepository.setIsHideInfoClicked(isHideInfoClicked)

    override fun isHideInfoClicked(): Flow<Boolean> = mainRadioStationRepository.isHideInfoClicked()
}