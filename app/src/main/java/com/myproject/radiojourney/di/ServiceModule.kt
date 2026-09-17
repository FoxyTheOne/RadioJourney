package com.myproject.radiojourney.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

// Часть API media3 помечена @UnstableApi: его могут изменить или удалить в новой версии media3 без предварительного deprecated.
// @OptIn - "знаем об этом, используем осознанно". На работу приложения аннотация не влияет, это только проверка Android Lint
@OptIn(UnstableApi::class) // DefaultMediaSourceFactory, setAllowCrossProtocolRedirects
@Module
@InstallIn(ServiceComponent::class)
object ServiceModule {

    // For our exoplayer (AudioAttributes from androidx.media3)
    @ServiceScoped // !!! ServiceScoped - это как синглтон внутри сервиса (не всего приложения)
    @Provides
    fun provideAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    // Player to play our music
    @ServiceScoped
    @Provides
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes, // <- Инструкцию по созданию мы описали выше
        dataSourceFactory: DefaultDataSource.Factory
    ): ExoPlayer = ExoPlayer.Builder(context)
        // DefaultMediaSourceFactory сам выбирает источник: HLS для .m3u8 (MediaItem с MIME-типом APPLICATION_M3U8),
        // обычный поток для остальных. Раньше это делал FirebaseMusicSource.asMediaSourcePlaylist() вручную
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
        .setHandleAudioBecomingNoisy(true) // Stops music if user plugs out headphones, for instance. It can be too noisy
        // Не даём телефону "уснуть" (Wi-Fi и процессор), пока играет интернет-радио с выключенным экраном
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()


    // Для обычных (не HLS) потоков. Http-источник - с User-Agent и редиректами между http и https (см. provideHttpDataSource)
    @Provides
    fun provideDataSourceFactory(
        @ApplicationContext context: Context,
        httpDataSourceFactory: DefaultHttpDataSource.Factory
    ) = DefaultDataSource.Factory(context, httpDataSourceFactory)

    // User-Agent: без него отправляется "Dalvik/2.1.0 (Linux; ...)", и часть серверов (например, streaming.live365.com)
    // отвечает 403 -> toast "Exoplayer can't read this url".
    // setAllowCrossProtocolRedirects: некоторые станции перенаправляют с http на https, а по умолчанию ExoPlayer такой редирект не выполняет
    @Provides
    fun provideHttpDataSource(
        @ApplicationContext context: Context
    ): DefaultHttpDataSource.Factory = DefaultHttpDataSource.Factory()
        .setUserAgent(Util.getUserAgent(context, "RadioJourney"))
        .setAllowCrossProtocolRedirects(true)
}