package com.myproject.radiojourney.di

import android.content.Context
import androidx.room.Room
import com.myproject.radiojourney.IAppSettings
import com.myproject.radiojourney.data.dataSource.local.auth.ILocalUserDataSource
import com.myproject.radiojourney.data.dataSource.local.auth.LocalUserDataSource
import com.myproject.radiojourney.data.dataSource.local.favorite.ILocalFavoriteDataSource
import com.myproject.radiojourney.data.dataSource.local.favorite.LocalFavoriteDataSource
import com.myproject.radiojourney.data.dataSource.local.radio.ILocalRadioDataSource
import com.myproject.radiojourney.data.dataSource.local.radio.LocalRadioDataSource
import com.myproject.radiojourney.data.dataSource.local.recommended.ILocalRecommendedDataSource
import com.myproject.radiojourney.data.dataSource.local.recommended.LocalRecommendedDataSource
import com.myproject.radiojourney.data.dataSource.network.INetworkRadioDataSource
import com.myproject.radiojourney.data.dataSource.network.NetworkRadioDataSource
import com.myproject.radiojourney.data.dataSource.network.service.IRadioServiceWrapper
import com.myproject.radiojourney.data.dataSource.network.service.RadioServiceWrapper
import com.myproject.radiojourney.data.localDatabaseRoom.*
import com.myproject.radiojourney.data.repository.AuthRepository
import com.myproject.radiojourney.data.repository.FavoriteStationRepository
import com.myproject.radiojourney.data.repository.RadioStationRepository
import com.myproject.radiojourney.data.repository.RecommendedStationRepository
import com.myproject.radiojourney.data.sharedPreference.AppSharedPreference
import com.myproject.radiojourney.data.sharedPreference.IAppSharedPreference
import com.myproject.radiojourney.domain.favouriteList.FavouriteListUseCase
import com.myproject.radiojourney.domain.favouriteList.IFavouriteListUseCase
import com.myproject.radiojourney.domain.homeRadio.HomeRadioUseCase
import com.myproject.radiojourney.domain.homeRadio.IHomeRadioUseCase
import com.myproject.radiojourney.domain.firstScreenLoading.LoginScreenUseCase
import com.myproject.radiojourney.domain.firstScreenLoading.ILoginScreenUseCase
import com.myproject.radiojourney.domain.iRepository.IAuthRepository
import com.myproject.radiojourney.domain.iRepository.IFavoriteStationRepository
import com.myproject.radiojourney.domain.iRepository.IRadioStationRepository
import com.myproject.radiojourney.domain.iRepository.IRecommendedStationRepository
import com.myproject.radiojourney.domain.logOut.ILogOutUseCase
import com.myproject.radiojourney.domain.logOut.LogOutUseCase
import com.myproject.radiojourney.domain.radioList.IRadioListUseCase
import com.myproject.radiojourney.domain.radioList.RadioListUseCase
import com.myproject.radiojourney.domain.recommendedList.IRecommendedListUseCase
import com.myproject.radiojourney.domain.recommendedList.RecommendedListUseCase
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
            ).fallbackToDestructiveMigration().build()

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
    }

    @Binds
    abstract fun bindsFavouriteListInteractor(
        favouriteListInteractor: FavouriteListUseCase
    ): IFavouriteListUseCase

    @Binds
    abstract fun bindsLoginScreenInteractor(
        loginScreenInteractor: LoginScreenUseCase
    ): ILoginScreenUseCase

    @Binds
    abstract fun bindsHomeRadioInteractor(
        homeRadioInteractor: HomeRadioUseCase
    ): IHomeRadioUseCase

    @Binds
    abstract fun bindsLogOutInteractor(
        logOutInteractor: LogOutUseCase
    ): ILogOutUseCase

    @Binds
    abstract fun bindsRadioListInteractor(
        radioListInteractor: RadioListUseCase
    ): IRadioListUseCase

    @Binds
    abstract fun bindsRecommendedListInteractor(
        recommendedListInteractor: RecommendedListUseCase
    ): IRecommendedListUseCase

    @Binds
    abstract fun bindsAuthRepository(
        authRepository: AuthRepository
    ): IAuthRepository

    @Binds
    abstract fun bindsFavoriteStationRepository(
        favoriteStationRepository: FavoriteStationRepository
    ): IFavoriteStationRepository

    @Binds
    abstract fun bindsRadioStationRepository(
        radioStationRepository: RadioStationRepository
    ): IRadioStationRepository

    @Binds
    abstract fun bindsRecommendedStationRepository(
        recommendedStationRepository: RecommendedStationRepository
    ): IRecommendedStationRepository

    @Binds
    abstract fun bindsLocalUserDataSource(
        localUserDataSource: LocalUserDataSource
    ): ILocalUserDataSource

    @Binds
    abstract fun bindsLocalFavoriteDataSource(
        localFavoriteDataSource: LocalFavoriteDataSource
    ): ILocalFavoriteDataSource

    @Binds
    abstract fun bindsLocalRecommendedDataSource(
        localRecommendedDataSource: LocalRecommendedDataSource
    ): ILocalRecommendedDataSource

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