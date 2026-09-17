package com.myproject.radiojourney.data.dataSource.network.service

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrofit-сервис для конкретного сервера radio-browser.
 *
 * Раньше при каждом запросе (и каждой попытке на другом сервере) создавались новые OkHttpClient и Retrofit.
 * У каждого OkHttpClient свой пул соединений и потоки, поэтому документация OkHttp советует один клиент на всё приложение.
 * Теперь клиент один (создаётся в SingletonModule), а Retrofit-сервис создаётся один раз на каждый адрес сервера
 */
@Singleton
class RadioServiceWrapper @Inject constructor(
    private val okHttpClient: OkHttpClient
) : IRadioServiceWrapper {

    private val radioServices = ConcurrentHashMap<String, IRadioService>()

    override fun getRadioService(baseURL: String): IRadioService =
        radioServices.getOrPut(baseURL) {
            Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build()
                .create(IRadioService::class.java)
        }
}