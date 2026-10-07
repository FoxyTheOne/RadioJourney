package com.myproject.radiojourney.domain.favouriteListUseCase

import com.myproject.radiojourney.domain.iRepository.IFavoriteStationRepository
import com.myproject.radiojourney.domain.model.RadioStation
import javax.inject.Inject

/**
 * Domain layer, UseCase избранного: отдаёт список станций со звездой в порядке "по стране, потом по названию".
 * Им пользуются и экран "Избранное", и плеер (плейлист избранного) - поэтому порядок у них одинаковый.
 * Сама звезда (добавить или убрать) - в общем ChangeFavouriteUseCase: её нажимают ещё на двух экранах
 *
 * Разделение обязанностей: репозиторий (data) только достаёт станции из базы, а в каком порядке их показывать -
 * правило приложения, и оно живёт здесь, в domain. Если когда-нибудь станции начнут храниться не в Room,
 * а, например, на сервере, порядок останется тем же: поменяется только репозиторий
 */
class FavouriteListUseCase @Inject constructor(
    private val favoriteStationRepository: IFavoriteStationRepository
) : IFavouriteListUseCase {

    override suspend fun getRadioStationFavouriteList(): List<RadioStation> =
        favoriteStationRepository.getFavoriteRadioStationList()
            .sortedWith(
                favouriteOrder(
                    countryCode = { it.countryCode },
                    stationName = { it.name })
            )
}