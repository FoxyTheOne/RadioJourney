package com.myproject.radiojourney.domain.homeRadioUseCase

import com.myproject.radiojourney.domain.iRepository.IMainRadioStationRepository
import com.myproject.radiojourney.entities.local.RadioStationLocal
import com.myproject.radiojourney.entities.presentation.CountryPresentation
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import com.myproject.radiojourney.utils.extension.removeLastNchars
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
    private val mainRadioStationRepository: IMainRadioStationRepository,
//    private val favoriteStationRepository: IFavoriteStationRepository
) : IHomeRadioUseCase {

    // local -> presentation. Оператор .map помогает перехватить данные и преобразовать их
    override fun subscribeOnCountryList(): Flow<List<CountryPresentation>> =
        mainRadioStationRepository.subscribeOnCountryList().map { countryLocalList ->
            val countryPresentationList = mutableListOf<CountryPresentation>()
            countryLocalList.forEach { countryLocal ->
                countryPresentationList.add(CountryPresentation.fromLocalToPresentation(countryLocal))
            }
            countryPresentationList
        }
            .flowOn(Dispatchers.IO) // Подписку и превращение делаем в другом потоке -> .flowOn(Dispatchers.IO)

    override suspend fun isRadioStationStored(): Boolean =
        mainRadioStationRepository.isRadioStationStored()

    override suspend fun getRadioStationUrl(): String? =
        mainRadioStationRepository.getRadioStationUrl()

    override suspend fun getRadioStationSaved(radioStationUrlResolved: String): RadioStationPresentation? {
        val radioStationLocalSaved: RadioStationLocal? =
            mainRadioStationRepository.getRadioStationSaved(radioStationUrlResolved)

        // local -> presentation
        var radioStationPresentationSaved: RadioStationPresentation? = null
        radioStationLocalSaved?.let {
            radioStationPresentationSaved =
                RadioStationPresentation.fromLocalToPresentation(radioStationLocalSaved)
        }

        return radioStationPresentationSaved
    }

    // Поменять в Shared Preference setIsRadioStationStored на true. Сохранить в Shared Preference (url)
    override suspend fun saveRadioStationUrl(isStored: Boolean, urlResolved: String) =
        mainRadioStationRepository.saveRadioStationUrl(isStored, urlResolved)

    // И сохранить радиостанцию в Room
    override suspend fun saveRadioStationInRoom(radioStation: RadioStationPresentation) {
        val radioStationLocal = RadioStationLocal.fromPresentationToLocal(radioStation)
        mainRadioStationRepository.saveRadioStationInRoom(radioStationLocal)
    }

//    <!-- claude 007
//    override suspend fun addStationInRoomToFavourites(currentRadioStation: RadioStationPresentation) {
//        if (currentRadioStation.countryCode.endsWith("_FAV")) {
//            val str: String = currentRadioStation.countryCode
//            val n = 4 // "_FAV" -> 4 chars
//
//            val newCountryCode = str.removeLastNchars(str, n)
//
//            currentRadioStation.countryCode = newCountryCode.toString()
//        }
//
//        val currentRadioStationLocal = RadioStationLocal.fromPresentationToLocal(
//            currentRadioStation,
//            isStationInFavourite = true
//        )
//        mainRadioStationRepository.saveRadioStationInRoom(currentRadioStationLocal)
//    }

//    override suspend fun deleteStationInRoomFromFavourite(currentRadioStation: RadioStationPresentation) {
//        if (currentRadioStation.countryCode.endsWith("_FAV")) {
//            val str: String = currentRadioStation.countryCode
//            val n = 4 // "_FAV" -> 4 chars
//
//            val newCountryCode = str.removeLastNchars(str, n)
//
//            currentRadioStation.countryCode = newCountryCode.toString()
//        }
//
//        val currentRadioStationLocal = RadioStationLocal.fromPresentationToLocal(
//            currentRadioStation,
//            isStationInFavourite = false
//        )
//        mainRadioStationRepository.saveRadioStationInRoom(currentRadioStationLocal)
//    }

    override suspend fun addStationInRoomToFavourites(currentRadioStation: RadioStationPresentation) {
        // В базу сохраняем код страны без "_FAV". Делаем копию: раньше менялся countryCode у самого объекта станции
        // из плейлиста плейера, и станция в плейлисте избранного переставала считаться станцией из избранного
        val currentRadioStationLocal = RadioStationLocal.fromPresentationToLocal(
            currentRadioStation.copy(countryCode = currentRadioStation.countryCode.removeSuffix("_FAV")),
            isStationInFavourite = true
        )
        mainRadioStationRepository.saveRadioStationInRoom(currentRadioStationLocal)
    }

    override suspend fun deleteStationInRoomFromFavourite(currentRadioStation: RadioStationPresentation) {
        // В базу сохраняем код страны без "_FAV". Делаем копию: раньше менялся countryCode у самого объекта станции
        // из плейлиста плейера, и станция в плейлисте избранного переставала считаться станцией из избранного
        val currentRadioStationLocal = RadioStationLocal.fromPresentationToLocal(
            currentRadioStation.copy(countryCode = currentRadioStation.countryCode.removeSuffix("_FAV")),
            isStationInFavourite = false
        )
        mainRadioStationRepository.saveRadioStationInRoom(currentRadioStationLocal)
    }

    // 007 claude -->

    override suspend fun setIsHideInfoClicked(isHideInfoClicked: Boolean) =
        mainRadioStationRepository.setIsHideInfoClicked(isHideInfoClicked)

    override suspend fun isHideInfoClicked(): Boolean =
        mainRadioStationRepository.isHideInfoClicked()

//    override suspend fun setRecommendedRadioStations(recommendedList: Map<String, String>) {
//        recommendedList.forEach {
//            val radioStation =
//                mainRadioStationRepository.getRadioStationSaved(it.key) // Ищем, может такая радиостанция уже сохранена в Room
//
//            if (radioStation != null) { // Если станция уже сохранена, меняем в ней isStationInRecommended = true
//                radioStation.isStationInRecommended = true
//                mainRadioStationRepository.saveRadioStationInRoom(radioStation)
//            } else {
//                // Если нет, скачиваем список радиостанций по нужной стране и в списке ищем нужную радиостанцию
//                val countryRadioStationsList =
//                    mainRadioStationRepository.getRadioStationList(it.value)
//                countryRadioStationsList.forEach { radioStationLocal ->
//                    if (radioStationLocal.urlResolved == it.key) {
//                        radioStationLocal.isStationInRecommended = true
//                        mainRadioStationRepository.saveRadioStationInRoom(radioStationLocal)
//                    }
//                }
//            }
//        }
//    }

}