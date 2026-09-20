package com.myproject.radiojourney.data.repository

import com.myproject.radiojourney.data.dataSource.local.favorite.ILocalFavoriteDataSource
import com.myproject.radiojourney.data.mapper.toDomain
import com.myproject.radiojourney.domain.iRepository.IFavoriteStationRepository
import com.myproject.radiojourney.domain.model.RadioStation
import javax.inject.Inject

/**
 * Data layer, Repository. Работает с Local и Remote data source.
 *
 * Repository - объект, предоставляющий доступ к данным с возможностью выбора источника данных в зависимости от условий.
 * Отдаёт наружу модели domain: преобразование local -> domain происходит здесь
 */
class FavoriteStationRepository @Inject constructor(
    private val localFavoriteDataSource: ILocalFavoriteDataSource
) : IFavoriteStationRepository {
    override suspend fun getFavoriteRadioStationList(): List<RadioStation> =
        localFavoriteDataSource.getFavoriteRadioStationList(isStationInFavorite = true)
            .map { it.toDomain() }
}