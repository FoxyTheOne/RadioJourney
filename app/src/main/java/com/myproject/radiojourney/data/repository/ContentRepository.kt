package com.myproject.radiojourney.data.repository

import com.myproject.radiojourney.domain.iRepository.IContentRepository

/**
 * Repository. Data layer. Работает с Local и Remote data source.
 * Подписка на локальную базу данных Room.
 * При работе с model, здесь происходит запрос в remote, преобразование remote -> local, сохранение результата в базу данных.
 */
class ContentRepository : IContentRepository {
}