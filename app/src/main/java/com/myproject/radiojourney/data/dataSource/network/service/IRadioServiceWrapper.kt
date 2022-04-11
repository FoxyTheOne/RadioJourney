package com.myproject.radiojourney.data.dataSource.network.service

interface IRadioServiceWrapper {
    fun getRadioService(baseURL: String): IRadioService
}