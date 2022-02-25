package com.myproject.radiojourney.di

import android.content.Context
import androidx.room.Room
import com.myproject.radiojourney.data.dataSource.local.ILocalAuthDataSource
import com.myproject.radiojourney.data.dataSource.local.LocalAuthDataSource
import com.myproject.radiojourney.data.localDatabaseRoom.AppRoomDBAbstract
import com.myproject.radiojourney.data.localDatabaseRoom.IUserDAO
import com.myproject.radiojourney.data.repository.AuthRepository
import com.myproject.radiojourney.data.sharedPreference.AppSharedPreference
import com.myproject.radiojourney.data.sharedPreference.IAppSharedPreference
import com.myproject.radiojourney.domain.SignInInteractor
import com.myproject.radiojourney.domain.ISignInInteractor
import com.myproject.radiojourney.domain.iAuthRepository.IAuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SingletonModule {

    companion object {
        // ROOM -> 1. База данных Room
        @Provides
        @Singleton
        fun providesAppDatabase(@ApplicationContext appContext: Context): AppRoomDBAbstract {
            val roomDatabase = Room.databaseBuilder(
                appContext,
                AppRoomDBAbstract::class.java,
                "AppRoomDatabase"
            ).build()

            return roomDatabase
        }
    }

    @Binds
    @Singleton
    abstract fun bindsSharedPreference(
        appSharedPreference: AppSharedPreference
    ) : IAppSharedPreference

}

@Module
@InstallIn(ViewModelComponent::class)
abstract class ViewModelModule {

    companion object {
        // ROOM -> 2. Объекты для обращения к Dao
        @Provides
        fun providesUserDAO(appDatabase: AppRoomDBAbstract): IUserDAO {
            return appDatabase.getUserDAO()
        }
    }

    @Binds
    abstract fun bindsAuthInteractor(
        signInInteractor: SignInInteractor
    ) : ISignInInteractor

    @Binds
    abstract fun bindsAuthRepository(
        authRepository: AuthRepository
    ) : IAuthRepository

    @Binds
    abstract fun bindsLocalAuthDataSource(
        localAuthDataSource: LocalAuthDataSource
    ) : ILocalAuthDataSource

}