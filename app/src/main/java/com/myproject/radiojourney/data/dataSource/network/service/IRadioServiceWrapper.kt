package com.myproject.radiojourney.data.dataSource.network.service

/**
 * Создание Retrofit-сервиса для конкретного сервера radio-browser.
 *
 * Адрес сервера заранее неизвестен: он выбирается из списка, который отдаёт DNS (см. NetworkRadioDataSource),
 * поэтому Retrofit нельзя создать один раз в модуле Hilt - его создаёт этот класс, по одному на адрес
 */
interface IRadioServiceWrapper {
    fun getRadioService(baseURL: String): IRadioService
}