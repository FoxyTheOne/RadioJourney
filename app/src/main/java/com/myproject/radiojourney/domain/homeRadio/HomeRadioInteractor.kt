package com.myproject.radiojourney.domain.homeRadio

import com.myproject.radiojourney.domain.iRepository.IContentRepository
import com.myproject.radiojourney.model.local.RadioStationFavouriteLocal
import com.myproject.radiojourney.model.local.RadioStationLocal
import com.myproject.radiojourney.model.presentation.CountryPresentation
import com.myproject.radiojourney.model.presentation.RadioStationPresentation
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
    // local -> presentation
    // Оператор .map помогает перехватить данные и преобразовать их
    override fun subscribeOnCountryList(): Flow<List<CountryPresentation>> =
        contentRepository.subscribeOnCountryList().map { countryLocalList ->
            val countryPresentationList = mutableListOf<CountryPresentation>()

            countryLocalList.forEach { countryLocal ->
                countryPresentationList.add(CountryPresentation.fromLocalToPresentation(countryLocal))
            }

            countryPresentationList
        }
            .flowOn(Dispatchers.IO) // Подписку и превращение делаем в другом потоке -> .flowOn(Dispatchers.IO)

    override suspend fun isRadioStationStored(): Boolean = contentRepository.isRadioStationStored()

    override suspend fun getRadioStationUrl(): String? = contentRepository.getRadioStationUrl()

    override suspend fun getRadioStationSaved(radioStationUrl: String): RadioStationPresentation? {
        val radioStationLocalSaved: RadioStationLocal? =
            contentRepository.getRadioStationSaved(radioStationUrl)

        // local -> presentation
        var radioStationPresentationSaved: RadioStationPresentation? = null

        radioStationLocalSaved?.let {
            radioStationPresentationSaved =
                RadioStationPresentation.fromLocalToPresentation(radioStationLocalSaved)
        }

        return radioStationPresentationSaved
    }

    override suspend fun saveRadioStationUrl(
        isStored: Boolean,
        radioStation: RadioStationPresentation
    ) =
        contentRepository.saveRadioStationUrl(isStored, radioStation)

    override suspend fun saveFavouriteRadioStationUrl(isStored: Boolean, url: String) =
        contentRepository.saveFavouriteRadioStationUrl(isStored, url)

    override suspend fun getToken(): Int? = contentRepository.getToken()

    override suspend fun addStationToFavourites(
        userCreatorIdInt: Int,
        currentRadioStation: RadioStationPresentation
    ) {
        val currentRadioStationFavouriteLocal =
            RadioStationFavouriteLocal.fromPresentationToFavouriteLocal(
                currentRadioStation,
                userCreatorIdInt,
                isStationInFavourite = true
            )
        contentRepository.addStationToFavourites(currentRadioStationFavouriteLocal)
    }

    override suspend fun isStationInFavourites(url: String): Boolean =
        contentRepository.isStationInFavourites(url)

    override suspend fun deleteRadioStationFromFavourite(
        userCreatorIdInt: Int,
        currentRadioStation: RadioStationPresentation
    ) {
        val currentRadioStationFavouriteLocal =
            RadioStationFavouriteLocal.fromPresentationToFavouriteLocal(
                currentRadioStation,
                userCreatorIdInt,
                isStationInFavourite = false
            )
        contentRepository.deleteRadioStationFromFavourite(currentRadioStationFavouriteLocal)
    }

}