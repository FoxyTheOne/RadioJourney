package com.myproject.radiojourney.data.dataSource.network

import com.myproject.radiojourney.model.remote.CountryCodeRemote

interface INetworkRadioDataSource {
    suspend fun getCountryCodeList(): List<CountryCodeRemote>
}