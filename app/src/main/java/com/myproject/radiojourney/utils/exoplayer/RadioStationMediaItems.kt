package com.myproject.radiojourney.utils.exoplayer

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import com.myproject.radiojourney.domain.model.RadioStation

/**
 * Преобразование станции (domain) в MediaItem media3 и обратно.
 * Раньше MediaItem -> станция экрана делал MainRadioUseCase, и domain слой зависел от библиотеки плеера.
 * Это знание о media3 - часть плеерного слоя, поэтому оно здесь
 */

// Ключи extras станции (MediaMetadata.extras)
private const val EXTRA_URL_RESOLVED = "UrlResolved"
private const val EXTRA_CLICK_COUNT = "ClickCount"
private const val EXTRA_COUNTRY = "Country"

// Станция в формате media3 - один объект и для плеера, и для уведомления, и для экрана
fun RadioStation.toMediaItem(notificationCountryText: String, artworkUri: Uri): MediaItem {
    // Адрес потока (localConfiguration) media3 не передаёт из сервиса на экран (MediaBrowser),
    // поэтому для экрана дублируем его в extras. Там же - число прослушиваний и страна
    val extras = Bundle().apply {
        putString(EXTRA_URL_RESOLVED, urlResolved)
        putLong(EXTRA_CLICK_COUNT, clickCount.toLong())
        putString(EXTRA_COUNTRY, country)
    }

    val metadata = MediaMetadata.Builder()
        .setTitle(name) // station name
        .setDisplayTitle(name)
        .setArtist(notificationCountryText) // вторая строка уведомления: страна или "Избранное: страна"
        .setSubtitle(countryCode) // country code ("PL" или "PL_FAV")
        .setArtworkUri(artworkUri) // большая картинка в уведомлении
        .setIsPlayable(true)
        .setIsBrowsable(false)
        .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
        .setExtras(extras)
        .build()

    return MediaItem.Builder()
        .setMediaId(stationUuid)
        .setUri(urlResolved)
        // HLS (.m3u8) воспроизводится другим источником - DefaultMediaSourceFactory выберет его по MIME-типу
        .apply { if (urlResolved.endsWith(".m3u8")) setMimeType(MimeTypes.APPLICATION_M3U8) }
        .setMediaMetadata(metadata)
        .build()
}

// Станция из MediaItem, полученного экраном от сервиса. Признак избранного здесь неизвестен - его добавляет MainRadioUseCase
fun MediaItem.toRadioStation(): RadioStation {
    val extras = mediaMetadata.extras
    return RadioStation(
        stationUuid = mediaId,
        name = mediaMetadata.title.toString(),
        urlResolved = extras?.getString(EXTRA_URL_RESOLVED).orEmpty(),
        clickCount = extras?.getLong(EXTRA_CLICK_COUNT)?.toInt() ?: 0,
        country = extras?.getString(EXTRA_COUNTRY).orEmpty(),
        countryCode = mediaMetadata.subtitle.toString(),
        isFavourite = false
    )
}