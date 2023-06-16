package com.myproject.radiojourney.data.dataSource.network.service

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

class RadioServiceWrapper @Inject constructor(
    private val userAgentInterceptor: UserAgentInterceptor
) : IRadioServiceWrapper {

    override fun getRadioService(baseURL: String): IRadioService {
        val client = OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseURL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        return retrofit.create(IRadioService::class.java)
    }

}