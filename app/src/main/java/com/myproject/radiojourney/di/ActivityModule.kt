package com.myproject.radiojourney.di

import android.content.Context
import com.myproject.radiojourney.IAppSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext

@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {

    // Приравняем наш context к IAppSettings. Мы можем так сделать, т.к. наш activity расширяет IAppSettings
    @Provides
    fun providesAppSettings(@ActivityContext context: Context): IAppSettings {
        return (context as IAppSettings)
    }

}