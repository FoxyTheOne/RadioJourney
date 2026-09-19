package com.myproject.radiojourney.domain.homeRadioUseCase

import com.myproject.radiojourney.domain.iRepository.IMainRadioStationRepository
import com.myproject.radiojourney.entities.presentation.CountryPresentation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin. Работает только с Repository.
 *
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor).
 * При работе с model, здесь происходит преобразование local -> presentation, т.е.
 * преобразование моделей в модели нижнего уровня перед тем, как нижний уровень сможет их использовать.
 */
class HomeRadioUseCase @Inject constructor(
    private val mainRadioStationRepository: IMainRadioStationRepository
) : IHomeRadioUseCase {
    // local -> presentation. Оператор .map помогает перехватить данные и преобразовать их
    override fun subscribeOnCountryList(): Flow<List<CountryPresentation>> =
        mainRadioStationRepository.subscribeOnCountryList()
            .map { countryLocalList -> countryLocalList.map(CountryPresentation::fromLocalToPresentation) }
            .flowOn(Dispatchers.IO) // Подписку и превращение делаем в другом потоке -> .flowOn(Dispatchers.IO)

    override suspend fun setIsHideInfoClicked(isHideInfoClicked: Boolean) =
        mainRadioStationRepository.setIsHideInfoClicked(isHideInfoClicked)

    override suspend fun isHideInfoClicked(): Boolean =
        mainRadioStationRepository.isHideInfoClicked()
}