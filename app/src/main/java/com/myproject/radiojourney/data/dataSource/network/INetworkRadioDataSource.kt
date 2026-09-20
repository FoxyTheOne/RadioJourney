package com.myproject.radiojourney.data.dataSource.network

import com.myproject.radiojourney.data.dataSource.network.entity.CountryCodeRemote
import com.myproject.radiojourney.data.dataSource.network.entity.RadioStationRemote
import com.myproject.radiojourney.other.Resource

interface INetworkRadioDataSource {
    suspend fun getCountryCodeList(): List<CountryCodeRemote>

    //    suspend fun getRadioStationList(countryCode: String): List<RadioStationRemote>
    suspend fun getRadioStationList(countryCode: String): Resource<List<RadioStationRemote>>

    //    suspend fun getAllRadioStationsList(): List<RadioStationRemote>
    suspend fun sendGetRequestToMarkRadioStationAsPopular(stationUuid: String) : Boolean
}