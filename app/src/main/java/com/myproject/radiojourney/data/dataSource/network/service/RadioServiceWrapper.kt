package com.myproject.radiojourney.data.dataSource.network.service

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

class RadioServiceWrapper @Inject constructor(): IRadioServiceWrapper {

    override fun getRadioService(baseURL: String): IRadioService {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseURL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(IRadioService::class.java)
    }

}