package com.myproject.radiojourney.data.dataSource.network.service

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
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

    private val freshConnectionRadioServices = ConcurrentHashMap<String, IRadioService>()

    // Клиент, который не держит открытых соединений: каждый запрос - новое соединение.
    // Обычно OkHttp отправляет несколько запросов к одному серверу по одному соединению - так быстрее. Но там, где провайдер
    // пропускает только первые ~16-20 КБ каждого соединения, байты всех запросов складываются: маленький запасной запрос
    // (10 популярных станций) шёл по соединению, где уже прошёл список стран, и обрывался сразу.
    // newBuilder() берёт у основного клиента всё остальное: таймауты, User-Agent, потоки
    private val freshConnectionClient: OkHttpClient by lazy {
        okHttpClient.newBuilder()
            .connectionPool(ConnectionPool(0, 1, TimeUnit.SECONDS))
            .build()
    }

    override fun getRadioService(baseURL: String, freshConnection: Boolean): IRadioService {
        val services = if (freshConnection) freshConnectionRadioServices else radioServices
        return services.getOrPut(baseURL) {
            Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(if (freshConnection) freshConnectionClient else okHttpClient)
                .build()
                .create(IRadioService::class.java)
        }
    }
}