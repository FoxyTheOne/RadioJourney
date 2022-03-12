package com.myproject.radiojourney.domain.homeRadio

import com.myproject.radiojourney.domain.iRepository.IContentRepository
import com.myproject.radiojourney.model.local.CountryLocal
import com.myproject.radiojourney.model.presentation.CountryPresentation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Interactor. Domain layer. Работает только с Repository.
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor)
 * При работе с model, здесь происходит преобразование local -> presentation (опционально)
 */
class HomeRadioInteractor @Inject constructor(
    private val contentRepository: IContentRepository
) : IHomeRadioInteractor {
    override suspend fun getCountryListAndSaveToRoom() =
        contentRepository.getCountryListAndSaveToRoom()

    // local -> presentation
    // Оператор .map помогает перехватить данные и преобразовать их
    override fun subscribeOnCountryList(): Flow<List<CountryPresentation>> =
        contentRepository.subscribeOnCountryList().map { countryLocalList ->
            val countryPresentationList = mutableListOf<CountryPresentation>()

            countryLocalList.forEach { countryLocal ->
                countryPresentationList.add(CountryPresentation.fromLocalToPresentation(countryLocal))
            }

            countryPresentationList
        }.flowOn(Dispatchers.IO) // Подписку и превращение делаем в другом потоке -> .flowOn(Dispatchers.IO)
}