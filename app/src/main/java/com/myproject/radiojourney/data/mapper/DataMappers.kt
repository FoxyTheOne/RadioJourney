package com.myproject.radiojourney.data.mapper

import com.google.android.gms.maps.model.LatLng
import com.myproject.radiojourney.data.dataSource.network.entity.RadioStationRemote
import com.myproject.radiojourney.data.localDatabaseRoom.entity.CountryLocal
import com.myproject.radiojourney.data.localDatabaseRoom.entity.MyStationLocal
import com.myproject.radiojourney.data.localDatabaseRoom.entity.RadioStationLocal
import com.myproject.radiojourney.data.localDatabaseRoom.entity.SavedStationLocal
import com.myproject.radiojourney.domain.model.Country
import com.myproject.radiojourney.domain.model.RadioStation
import com.myproject.radiojourney.other.Constants.MY_STATIONS_COUNTRY_CODE

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

// Своя станция пользователя. Кода страны у неё нет, поэтому ставим служебный MY_STATIONS_COUNTRY_CODE:
// по нему плеер и экраны понимают, что это плейлист "Мои радиостанции"
fun MyStationLocal.toDomain() = RadioStation(
    stationUuid = stationUuid,
    name = stationName,
    urlResolved = urlResolved,
    clickCount = 0, // число прослушиваний есть только у станций каталога radio-browser
    country = "",
    countryCode = MY_STATIONS_COUNTRY_CODE,
    isFavourite = false
)

fun RadioStation.toMyStationLocal(addedAt: Long = System.currentTimeMillis()) = MyStationLocal(
    stationUuid = stationUuid,
    stationName = name,
    urlResolved = urlResolved,
    addedAt = addedAt
)

// Станция сохранённого списка страны (см. SavedStationLocal). Признак избранного не храним:
// его, как и у свежего списка, проставляют по таблице избранного уже потом
fun SavedStationLocal.toDomain() = RadioStation(
    stationUuid = stationUuid,
    name = stationName,
    urlResolved = urlResolved,
    clickCount = clickCount,
    country = country,
    countryCode = stationCountryCode,
    isFavourite = false
)

fun RadioStation.toSavedStationLocal(listCountryCode: String, position: Int, savedAt: Long) =
    SavedStationLocal(
        countryCode = listCountryCode,
        stationUuid = stationUuid,
        stationName = name,
        urlResolved = urlResolved,
        clickCount = clickCount,
        country = country,
        stationCountryCode = countryCode,
        position = position,
        savedAt = savedAt
    )