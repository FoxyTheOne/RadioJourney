package com.myproject.radiojourney.data.mapper

import com.google.android.gms.maps.model.LatLng
import com.myproject.radiojourney.data.dataSource.network.entity.RadioStationRemote
import com.myproject.radiojourney.data.localDatabaseRoom.entity.CountryLocal
import com.myproject.radiojourney.data.localDatabaseRoom.entity.RadioStationLocal
import com.myproject.radiojourney.domain.model.Country
import com.myproject.radiojourney.domain.model.RadioStation

/**
 * Преобразования моделей data слоя (Room, сервер) в модели domain и обратно.
 * Раньше эти функции были в companion object самих сущностей, и сущность Room знала о модели экрана (fromPresentationToLocal)
 */

fun RadioStationRemote.toDomain() = RadioStation(
    stationUuid = stationuuid,
    name = name,
    urlResolved = url_resolved,
    clickCount = clickcount,
    country = country,
    countryCode = countrycode,
    isFavourite = false // с сервера приходит без признака избранного - его знает только Room
)

fun RadioStationLocal.toDomain() = RadioStation(
    stationUuid = stationuuid,
    name = stationName,
    urlResolved = urlResolved,
    clickCount = clickCount,
    country = country,
    countryCode = countryCode,
    isFavourite = isStationInFavourite
)

fun RadioStation.toLocal() = RadioStationLocal(
    stationuuid = stationUuid,
    urlResolved = urlResolved,
    stationName = name,
    clickCount = clickCount,
    country = country,
    countryCode = countryCode,
    isStationInFavourite = isFavourite,
    isStationInRecommended = false // экран рекомендаций удалён, столбец остался в таблице
)

fun CountryLocal.toDomain() = Country(
    countryCode = countryCode,
    stationCount = stationcount,
    countryName = countryName,
    latitude = countryLocation.latitude,
    longitude = countryLocation.longitude
)

fun Country.toLocal() = CountryLocal(
    countryCode = countryCode,
    stationcount = stationCount,
    countryName = countryName,
    countryLocation = LatLng(latitude, longitude)
)