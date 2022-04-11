package com.myproject.radiojourney.di

import android.content.Context
import androidx.room.Room
import com.myproject.radiojourney.IAppSettings
import com.myproject.radiojourney.data.dataSource.local.auth.ILocalAuthDataSource
import com.myproject.radiojourney.data.dataSource.local.auth.LocalAuthDataSource
import com.myproject.radiojourney.data.dataSource.local.radio.ILocalRadioDataSource
import com.myproject.radiojourney.data.dataSource.local.radio.LocalRadioDataSource
import com.myproject.radiojourney.data.dataSource.network.INetworkRadioDataSource
import com.myproject.radiojourney.data.dataSource.network.NetworkRadioDataSource
import com.myproject.radiojourney.data.dataSource.network.service.IRadioServiceWrapper
import com.myproject.radiojourney.data.dataSource.network.service.RadioServiceWrapper
import com.myproject.radiojourney.data.localDatabaseRoom.*
import com.myproject.radiojourney.data.repository.AuthRepository
import com.myproject.radiojourney.data.repository.ContentRepository
import com.myproject.radiojourney.data.sharedPreference.AppSharedPreference
import com.myproject.radiojourney.data.sharedPreference.IAppSharedPreference
import com.myproject.radiojourney.domain.favouriteList.FavouriteListInteractor
import com.myproject.radiojourney.domain.favouriteList.IFavouriteListInteractor
import com.myproject.radiojourney.domain.homeRadio.HomeRadioInteractor
import com.myproject.radiojourney.domain.homeRadio.IHomeRadioInteractor
import com.myproject.radiojourney.domain.signIn.SignInInteractor
import com.myproject.radiojourney.domain.signIn.ISignInInteractor
import com.myproject.radiojourney.domain.iRepository.IAuthRepository
import com.myproject.radiojourney.domain.iRepository.IContentRepository
import com.myproject.radiojourney.domain.logOut.ILogOutInteractor
import com.myproject.radiojourney.domain.logOut.LogOutInteractor
import com.myproject.radiojourney.domain.radioList.IRadioListInteractor
import com.myproject.radiojourney.domain.radioList.RadioListInteractor
import com.myproject.radiojourney.domain.signUp.ISignUpInteractor
import com.myproject.radiojourney.domain.signUp.SignUpInteractor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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

        @Provides
        fun providesCountryDAO(appDatabase: AppRoomDBAbstract): ICountryDAO {
            return appDatabase.getCountryDAO()
        }

        @Provides
        fun providesRadioStationDAO(appDatabase: AppRoomDBAbstract): IRadioStationDAO {
            return appDatabase.getRadioStationDAO()
        }

        @Provides
        fun providesRadioStationFavouriteDAO(appDatabase: AppRoomDBAbstract): IRadioStationFavouriteDAO {
            return appDatabase.getRadioStationFavouriteDAO()
        }
    }

    @Binds
    @Singleton
    abstract fun bindsSharedPreference(
        appSharedPreference: AppSharedPreference
    ): IAppSharedPreference

    @Binds
    abstract fun bindsLocalRadioDataSource(
        localRadioDataSource: LocalRadioDataSource
    ): ILocalRadioDataSource

    @Binds
    abstract fun bindsNetworkRadioDataSource(
        networkRadioDataSource: NetworkRadioDataSource
    ): INetworkRadioDataSource

    @Binds
    abstract fun bindRadioServiceWrapper(
        radioServiceWrapper: RadioServiceWrapper
    ): IRadioServiceWrapper

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

        // Переношу следующие конструкторы в SingletonModule, т.к. их будет использовать LocalRadioDataSource, который использует Foreground service
//        @Provides
//        fun providesCountryDAO(appDatabase: AppRoomDBAbstract): ICountryDAO {
//            return appDatabase.getCountryDAO()
//        }

//        @Provides
//        fun providesRadioStationDAO(appDatabase: AppRoomDBAbstract): IRadioStationDAO {
//            return appDatabase.getRadioStationDAO()
//        }

//        @Provides
//        fun providesRadioStationFavouriteDAO(appDatabase: AppRoomDBAbstract): IRadioStationFavouriteDAO {
//            return appDatabase.getRadioStationFavouriteDAO()
//        }
    }

    @Binds
    abstract fun bindsSignInInteractor(
        signInInteractor: SignInInteractor
    ): ISignInInteractor

    @Binds
    abstract fun bindsSignUpInteractor(
        signUpInteractor: SignUpInteractor
    ): ISignUpInteractor

    @Binds
    abstract fun bindsLogOutInteractor(
        logOutInteractor: LogOutInteractor
    ): ILogOutInteractor

    @Binds
    abstract fun bindsHomeRadioInteractor(
        homeRadioInteractor: HomeRadioInteractor
    ): IHomeRadioInteractor

    @Binds
    abstract fun bindsRadioListInteractor(
        radioListInteractor: RadioListInteractor
    ): IRadioListInteractor

    @Binds
    abstract fun bindsIFavouriteListInteractor(
        favouriteListInteractor: FavouriteListInteractor
    ): IFavouriteListInteractor

    @Binds
    abstract fun bindsAuthRepository(
        authRepository: AuthRepository
    ): IAuthRepository

    @Binds
    abstract fun bindsContentRepository(
        contentRepository: ContentRepository
    ): IContentRepository

    @Binds
    abstract fun bindsLocalAuthDataSource(
        localAuthDataSource: LocalAuthDataSource
    ): ILocalAuthDataSource

    // Переношу следующие конструкторы в SingletonModule, т.к. их будет использовать LocalRadioDataSource, который использует Foreground service
//    @Binds
//    abstract fun bindsLocalRadioDataSource(
//        localRadioDataSource: LocalRadioDataSource
//    ) : ILocalRadioDataSource
//
//    @Binds
//    abstract fun bindsNetworkRadioDataSource(
//        networkRadioDataSource: NetworkRadioDataSource
//    ) : INetworkRadioDataSource

//    @Binds
//    abstract fun bindRadioServiceWrapper(
//        radioServiceWrapper: RadioServiceWrapper
//    ): IRadioServiceWrapper

}

@Module
@InstallIn(ActivityComponent::class)
class ActivityModule {

    // Приравняем наш context к IAppSettings. Мы можем так сделать, т.к. наш activity расширяет IAppSettings
    @Provides
    fun providesAppSettings(@ActivityContext context: Context): IAppSettings {
        return (context as IAppSettings)
    }

}