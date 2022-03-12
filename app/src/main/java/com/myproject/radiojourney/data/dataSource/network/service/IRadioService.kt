package com.myproject.radiojourney.data.dataSource.network.service

import com.myproject.radiojourney.model.remote.CountryCodeRemote
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit2
 * 1. Превращаем JSON в объекты и сохраняем в папку model -> remote
 * 2. Запрос нам нужно будет делать по динамической BASE_URL. Поэтому retrofit инициал. в hilt не на прямую, а с помощью "обёртки".
 *  Составляем end point нашего запроса, создаём для этого интерфейс (IRadioService)
 * 3. Создаём обертку, которую будем инициализировать с помощью hilt (класс RadioServiceWrapper и интерфейс).
 * В ней создаём retrofit сервис.
 * 4. Описываем метод в файле Module для hilt
 */
interface IRadioService {
    // Пример - http://de1.api.radio-browser.info/json/countrycodes
    companion object {
        private const val BASE_PATH = "json"
        private const val COUNTRY_CODE_PATH = "countrycodes"
    }

    @GET("$BASE_PATH/{countrycodes}")
    suspend fun getCountryCodeList(
        @Path("countrycodes") countryCodes: String = COUNTRY_CODE_PATH
    ): List<CountryCodeRemote>

}