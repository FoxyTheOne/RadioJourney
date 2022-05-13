package com.myproject.radiojourney.data.dataSource.network

import com.myproject.radiojourney.entities.remote.CountryCodeRemote
import com.myproject.radiojourney.entities.remote.RadioStationRemote

interface INetworkRadioDataSource {
    suspend fun getCountryCodeList(): List<CountryCodeRemote>
    suspend fun getRadioStationList(countryCode: String): List<RadioStationRemote>
    suspend fun getAllRadioStationsList(): List<RadioStationRemote>
}