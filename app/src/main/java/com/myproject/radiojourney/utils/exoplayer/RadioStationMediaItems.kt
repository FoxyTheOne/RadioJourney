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

// Название песни, которое сейчас показывается у станции (null - станция его не прислала)
val MediaMetadata.nowPlayingTitle: String?
    get() = extras?.getString(EXTRA_NOW_PLAYING)

// Ключи extras станции (MediaMetadata.extras)
private const val EXTRA_URL_RESOLVED = "UrlResolved"
private const val EXTRA_CLICK_COUNT = "ClickCount"
private const val EXTRA_COUNTRY = "Country"

// Вторая строка уведомления без песни ("Польша" или "Избранное: Польша") - чтобы вернуть её, когда название песни пропало
private const val EXTRA_NOTIFICATION_TEXT = "NotificationText"

// Название песни, которое станция прислала в потоке (см. NowPlaying.kt). Нет ключа - названия нет
private const val EXTRA_NOW_PLAYING = "NowPlaying"

// Станция в формате media3 - один объект и для плеера, и для уведомления, и для экрана
fun RadioStation.toMediaItem(notificationCountryText: String, artworkUri: Uri): MediaItem {
    // Адрес потока (localConfiguration) media3 не передаёт из сервиса на экран (MediaBrowser),
    // поэтому для экрана дублируем его в extras. Там же - число прослушиваний и страна
    val extras = Bundle().apply {
        putString(EXTRA_URL_RESOLVED, urlResolved)
        putLong(EXTRA_CLICK_COUNT, clickCount.toLong())
        putString(EXTRA_COUNTRY, country)
        putString(EXTRA_NOTIFICATION_TEXT, notificationCountryText)
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

/**
 * Та же станция с другим названием песни (null - названия нет).
 * Название песни - во второй строке уведомления и на экране блокировки (artist): так делают все музыкальные приложения,
 * и его же покажут часы, Android Auto и магнитола в машине. Когда названия нет, там снова страна.
 * Для нижнего плеера в приложении название дублируется в extras: по artist экран не отличил бы песню от страны
 */
fun MediaItem.withNowPlaying(title: String?): MediaItem {
    val extras = Bundle(mediaMetadata.extras ?: Bundle()).apply {
        if (title != null) putString(EXTRA_NOW_PLAYING, title) else remove(EXTRA_NOW_PLAYING)
    }
    val metadata = mediaMetadata.buildUpon()
        .setArtist(title ?: extras.getString(EXTRA_NOTIFICATION_TEXT))
        .setExtras(extras)
        .build()
    return buildUpon().setMediaMetadata(metadata).build()
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