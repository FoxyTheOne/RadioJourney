package com.myproject.radiojourney.domain.homeRadio

import com.myproject.radiojourney.domain.iRepository.IFavoriteStationRepository
import com.myproject.radiojourney.domain.iRepository.IRadioStationRepository
import com.myproject.radiojourney.entities.local.RadioStationLocal
import com.myproject.radiojourney.entities.presentation.CountryPresentation
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
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
    private val radioStationRepository: IRadioStationRepository,
    private val favoriteStationRepository: IFavoriteStationRepository
) : IHomeRadioUseCase {

    // local -> presentation. Оператор .map помогает перехватить данные и преобразовать их
    override fun subscribeOnCountryList(): Flow<List<CountryPresentation>> =
        radioStationRepository.subscribeOnCountryList().map { countryLocalList ->
            val countryPresentationList = mutableListOf<CountryPresentation>()
            countryLocalList.forEach { countryLocal ->
                countryPresentationList.add(CountryPresentation.fromLocalToPresentation(countryLocal))
            }
            countryPresentationList
        }
            .flowOn(Dispatchers.IO) // Подписку и превращение делаем в другом потоке -> .flowOn(Dispatchers.IO)

    override suspend fun isRadioStationStored(): Boolean =
        radioStationRepository.isRadioStationStored()

    override suspend fun getRadioStationUrl(): String? = radioStationRepository.getRadioStationUrl()

    override suspend fun getRadioStationSaved(radioStationUrl: String): RadioStationPresentation? {
        val radioStationLocalSaved: RadioStationLocal? =
            radioStationRepository.getRadioStationSaved(radioStationUrl)

        // local -> presentation
        var radioStationPresentationSaved: RadioStationPresentation? = null
        radioStationLocalSaved?.let {
            radioStationPresentationSaved =
                RadioStationPresentation.fromLocalToPresentation(radioStationLocalSaved)
        }

        return radioStationPresentationSaved
    }

    // Поменять в Shared Preference setIsRadioStationStored на true. Сохранить в Shared Preference (url)
    override suspend fun saveRadioStationUrl(isStored: Boolean, url: String) =
        radioStationRepository.saveRadioStationUrl(isStored, url)

    // И сохранить радиостанцию в Room
    override suspend fun saveRadioStationInRoom(radioStation: RadioStationPresentation) {
        val radioStationLocal = RadioStationLocal.fromPresentationToLocal(radioStation)
        radioStationRepository.saveRadioStationInRoom(radioStationLocal)
    }

    override suspend fun addStationInRoomToFavourites(currentRadioStation: RadioStationPresentation) {
        val currentRadioStationLocal = RadioStationLocal.fromPresentationToLocal(
            currentRadioStation,
            isStationInFavourite = true
        )
        radioStationRepository.saveRadioStationInRoom(currentRadioStationLocal)
    }

    override suspend fun deleteStationInRoomFromFavourite(currentRadioStation: RadioStationPresentation) {
        val currentRadioStationLocal = RadioStationLocal.fromPresentationToLocal(
            currentRadioStation,
            isStationInFavourite = false
        )
        radioStationRepository.saveRadioStationInRoom(currentRadioStationLocal)
    }

    override suspend fun setRecommendedRadioStations(recommendedList: Map<String, String>) {
        recommendedList.forEach {
            val radioStation =
                radioStationRepository.getRadioStationSaved(it.key) // Ищем, может такая радиостанция уже сохранена в Room

            if (radioStation != null) { // Если станция уже сохранена, меняем в ней isStationInRecommended = true
                radioStation.isStationInRecommended = true
                radioStationRepository.saveRadioStationInRoom(radioStation)
            } else {
                // Если нет, скачиваем список радиостанций по нужной стране и в списке ищем нужную радиостанцию
                val countryRadioStationsList = radioStationRepository.getRadioStationList(it.value)
                countryRadioStationsList.forEach { radioStationLocal ->
                    if (radioStationLocal.url == it.key) {
                        radioStationLocal.isStationInRecommended = true
                        radioStationRepository.saveRadioStationInRoom(radioStationLocal)
                    }
                }
            }
        }
    }

}