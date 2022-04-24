package com.myproject.radiojourney.data.repository

import com.myproject.radiojourney.data.dataSource.local.favorite.ILocalFavoriteDataSource
import com.myproject.radiojourney.domain.iRepository.IFavoriteStationRepository
import com.myproject.radiojourney.model.local.RadioStationLocal
import javax.inject.Inject

/**
 * Data layer, Repository. Работает с Local и Remote data source.
 *
 * Repository - объект, предоставляющий доступ к данным с возможностью выбора источника данных в зависимости от условий.
 * Подписка на локальную базу данных Room. Раскладываем данные.
 * При работе с model, здесь происходит запрос в remote, преобразование remote -> local, сохранение результата в базу данных.
 */
class FavoriteStationRepository @Inject constructor(
    private val localFavoriteDataSource: ILocalFavoriteDataSource
) : IFavoriteStationRepository {
    override suspend fun getFavoriteRadioStationList(isStationInFavorite: Boolean): List<RadioStationLocal> =
        localFavoriteDataSource.getFavoriteRadioStationList(isStationInFavorite)
}