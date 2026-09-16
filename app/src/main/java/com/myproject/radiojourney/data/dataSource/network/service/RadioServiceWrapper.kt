package com.myproject.radiojourney.data.dataSource.network.service

import com.myproject.radiojourney.other.Constants.NETWORK_CALL_TIMEOUT
import com.myproject.radiojourney.other.Constants.NETWORK_CONNECT_TIMEOUT
import com.myproject.radiojourney.other.Constants.NETWORK_READ_TIMEOUT
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class RadioServiceWrapper @Inject constructor(
    private val userAgentInterceptor: UserAgentInterceptor
) : IRadioServiceWrapper {

    override fun getRadioService(baseURL: String): IRadioService {
        val client = OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)

            // 006 claude:
            // Без явных таймаутов попытка к недоступному серверу длилась ~20 с (по 10 с на IPv6 и IPv4 адрес),
            // и за время полосы загрузки успевали пройти всего 2-3 попытки
            .connectTimeout(NETWORK_CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(NETWORK_READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .callTimeout(NETWORK_CALL_TIMEOUT, TimeUnit.MILLISECONDS) // весь запрос целиком, включая скачивание списка

            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseURL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        return retrofit.create(IRadioService::class.java)
    }

}