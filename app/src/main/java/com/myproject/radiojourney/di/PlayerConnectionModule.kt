package com.myproject.radiojourney.di

import android.content.Context
import com.myproject.radiojourney.utils.exoplayer.MusicServiceConnection
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Связь экранов с сервисом плеера (MusicServiceConnection): через неё MainViewModel узнаёт, что играет,
 * и отправляет команды "играть", "пауза", "следующая станция".
 *
 * Чем отличается от ServiceModule: там то, что живёт внутри сервиса (ExoPlayer) и пересоздаётся вместе с ним.
 * А связь нужна экранам всё время, пока открыто приложение, поэтому она одна на всё приложение (@Singleton).
 * Раньше она лежала в общем модуле вместе с базой данных и сетью, хотя к слою data не относится
 */
@Module
@InstallIn(SingletonComponent::class)
object PlayerConnectionModule {

    @Provides
    @Singleton
    fun providesMusicServiceConnection(@ApplicationContext context: Context) =
        MusicServiceConnection(context)
}