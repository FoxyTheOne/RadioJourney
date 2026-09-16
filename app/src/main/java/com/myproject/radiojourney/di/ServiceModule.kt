package com.myproject.radiojourney.di

import android.content.Context
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@Module
@InstallIn(ServiceComponent::class)
object ServiceModule {

    // For our exoplayer (AudioAttributes from google, exoplayer2)
    @ServiceScoped // !!! ServiceScoped - это как синглтон внутри сервиса (не всего приложения)
    @Provides
    fun provideAudioAttributes() = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC) // CONTENT_TYPE_MUSIC is deprecated
        .setUsage(C.USAGE_MEDIA)
        .build()

    // Player to play our music
    @ServiceScoped
    @Provides
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes // <- Инструкцию по созданию мы описали выше
    ) = ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(audioAttributes, true)
        setHandleAudioBecomingNoisy(true) // Stops music if user plugs in his headphones, for instance. It can be too noisy
    }

    // Deprecated
//    @ServiceScoped
//    @Provides
//    fun provideDataSourceFactory(
//        @ApplicationContext context: Context
//    ) = DefaultDataSourceFactory(context, Util.getUserAgent(context, "RadioJourney"))@ServiceScoped


    // <!-- 004 claude
//    @Provides
//    fun provideDataSourceFactory(
//        @ApplicationContext context: Context
//    ) = DefaultDataSource.Factory(context)
//
//    @Provides
//    fun provideHttpDataSource() = DefaultHttpDataSource.Factory()

    // Для обычных (не HLS) потоков. Раньше DefaultDataSource.Factory(context) создавал внутри свой http-источник -
    // без User-Agent и без редиректов между http и https (см. provideHttpDataSource)
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
    // 004 claude -->

}