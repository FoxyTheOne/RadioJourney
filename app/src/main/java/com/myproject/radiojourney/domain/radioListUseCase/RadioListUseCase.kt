package com.myproject.radiojourney.domain.radioListUseCase

import android.util.Log
import com.myproject.radiojourney.data.repository.MainRadioStationRepository
import com.myproject.radiojourney.domain.iRepository.IMainRadioStationRepository
import com.myproject.radiojourney.entities.local.RadioStationLocal
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import com.myproject.radiojourney.other.Constants
import com.myproject.radiojourney.other.Resource
import com.myproject.radiojourney.other.Status
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin. Работает только с Repository.
 *
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor).
 * При работе с model, здесь происходит преобразование local -> presentation, т.е.
 * преобразование моделей в модели нижнего уровня перед тем, как нижний уровень сможет их использовать.
 */
class RadioListUseCase @Inject constructor(
    private val mainRadioStationRepository: IMainRadioStationRepository
) : IRadioListUseCase {
    //    override suspend fun getRadioStationList(countryCode: String): List<RadioStationPresentation> {
//        val radioStationLocalList = mainRadioStationRepository.getRadioStationList(countryCode)
//
//        // Преобразуем модельки local -> presentation
//        val radioStationPresentationList = mutableListOf<RadioStationPresentation>()
//
//        radioStationLocalList.forEach { radioStationLocal ->
//            val radioStationPresentation =
//                RadioStationPresentation.fromLocalToPresentation(radioStationLocal)
//            radioStationPresentationList.add(radioStationPresentation)
//        }
//
//        return radioStationPresentationList.toList()
//    }
    override suspend fun getRadioStationList(countryCode: String): Resource<List<RadioStationPresentation>> {
        // Получаем список радиостанций из mainRadioStationRepository в формате Resource чтобы знать ответ с сервера
        val radioStationLocalListResource =
            mainRadioStationRepository.getRadioStationList(countryCode)

        // Если была ошибка HttpException, обозначаем по умолчанию
        var radioStationPresentationListResource: Resource<List<RadioStationPresentation>> =
            Resource.error(Constants.SERVER_IS_DOWN, listOf())

        // Далее проверяем и если ошибки HttpException не было - меняем значение
        radioStationLocalListResource.let { result ->
            when (result.status) {
                Status.SUCCESS -> {
                    result.data?.let { radioStationLocalList ->

                        // Преобразуем модельки local -> presentation
                        val radioStationPresentationList = mutableListOf<RadioStationPresentation>()

                        radioStationLocalList.forEach { radioStationLocal ->
                            val radioStationPresentation =
                                RadioStationPresentation.fromLocalToPresentation(radioStationLocal)
                            radioStationPresentationList.add(radioStationPresentation)
                        }

                        radioStationPresentationListResource =
                            Resource.success(radioStationPresentationList.toList())
                    }
                }

                Status.ERROR -> Unit // we don't need this
                Status.LOADING -> Unit // we don't need this
            }
        }
        return radioStationPresentationListResource
    }
}