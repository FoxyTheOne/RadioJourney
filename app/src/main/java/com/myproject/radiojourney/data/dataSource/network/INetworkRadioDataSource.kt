package com.myproject.radiojourney.data.dataSource.network

import com.myproject.radiojourney.model.remote.CountryCodeRemote
import com.myproject.radiojourney.model.remote.RadioStationRemote

interface INetworkRadioDataSource {
    suspend fun getCountryCodeList(): List<CountryCodeRemote>
    suspend fun getRadioStationList(countryCode: String): List<RadioStationRemote>
}