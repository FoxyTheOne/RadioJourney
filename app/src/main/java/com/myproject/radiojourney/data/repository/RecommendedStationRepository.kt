package com.myproject.radiojourney.data.repository

import com.myproject.radiojourney.data.dataSource.local.recommended.ILocalRecommendedDataSource
import com.myproject.radiojourney.domain.iRepository.IRecommendedStationRepository
import com.myproject.radiojourney.entities.local.RadioStationLocal
import javax.inject.Inject

/**
 * Data layer, Repository. Работает с Local и Remote data source.
 *
 * Repository - объект, предоставляющий доступ к данным с возможностью выбора источника данных в зависимости от условий.
 * Подписка на локальную базу данных Room. Раскладываем данные.
 * При работе с model, здесь происходит запрос в remote, преобразование remote -> local, сохранение результата в базу данных.
 */
class RecommendedStationRepository @Inject constructor(
    private val localRecommendedDataSource: ILocalRecommendedDataSource,
): IRecommendedStationRepository {
    override suspend fun getRecommendedRadioStationList(): List<RadioStationLocal> =
        localRecommendedDataSource.getRecommendedRadioStationList()
}