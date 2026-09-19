package com.myproject.radiojourney.di

import android.content.Context
import androidx.room.Room
import com.myproject.radiojourney.data.dataSource.local.auth.ILocalUserDataSource
import com.myproject.radiojourney.data.dataSource.local.auth.LocalUserDataSource
import com.myproject.radiojourney.data.dataSource.local.favorite.ILocalFavoriteDataSource
import com.myproject.radiojourney.data.dataSource.local.favorite.LocalFavoriteDataSource
import com.myproject.radiojourney.data.dataSource.local.radio.ILocalRadioDataSource
import com.myproject.radiojourney.data.dataSource.local.radio.LocalRadioDataSource
import com.myproject.radiojourney.data.dataSource.network.INetworkRadioDataSource
import com.myproject.radiojourney.data.dataSource.network.NetworkRadioDataSource
import com.myproject.radiojourney.data.dataSource.network.service.IRadioServiceWrapper
import com.myproject.radiojourney.data.dataSource.network.service.RadioServiceWrapper
import com.myproject.radiojourney.data.dataSource.network.service.UserAgentInterceptor
import com.myproject.radiojourney.data.localDatabaseRoom.*
import com.myproject.radiojourney.data.repository.AuthRepository
import com.myproject.radiojourney.data.repository.FavoriteStationRepository
import com.myproject.radiojourney.data.repository.MainRadioStationRepository
import com.myproject.radiojourney.data.sharedPreference.AppSharedPreference
import com.myproject.radiojourney.data.sharedPreference.IAppSharedPreference
import com.myproject.radiojourney.domain.favouriteListUseCase.FavouriteListUseCase
import com.myproject.radiojourney.domain.favouriteListUseCase.IFavouriteListUseCase
import com.myproject.radiojourney.domain.firstScreenLoadingUseCase.ILoginScreenUseCase
import com.myproject.radiojourney.domain.firstScreenLoadingUseCase.LoginScreenUseCase
import com.myproject.radiojourney.domain.homeRadioUseCase.HomeRadioUseCase
import com.myproject.radiojourney.domain.homeRadioUseCase.IHomeRadioUseCase
import com.myproject.radiojourney.domain.iRepository.IAuthRepository
import com.myproject.radiojourney.domain.iRepository.IFavoriteStationRepository
import com.myproject.radiojourney.domain.iRepository.IMainRadioStationRepository
import com.myproject.radiojourney.domain.logOutUseCase.ILogOutUseCase
import com.myproject.radiojourney.domain.logOutUseCase.LogOutUseCase
import com.myproject.radiojourney.domain.mainRadioUseCase.IMainRadioUseCase
import com.myproject.radiojourney.domain.mainRadioUseCase.MainRadioUseCase
import com.myproject.radiojourney.domain.radioListUseCase.IRadioListUseCase
import com.myproject.radiojourney.domain.radioListUseCase.RadioListUseCase
import com.myproject.radiojourney.utils.exoplayer.MusicServiceConnection
import com.myproject.radiojourney.other.Constants.NETWORK_CALL_TIMEOUT
import com.myproject.radiojourney.other.Constants.NETWORK_CONNECT_TIMEOUT
import com.myproject.radiojourney.other.Constants.NETWORK_READ_TIMEOUT
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import com.myproject.radiojourney.domain.changeFavouriteUseCase.ChangeFavouriteUseCase
import com.myproject.radiojourney.domain.changeFavouriteUseCase.IChangeFavouriteUseCase

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
        @Singleton
        fun providesMusicServiceConnection(
            @ApplicationContext context: Context
        ) =
            MusicServiceConnection(context) // Создаём экземпляр нашего класса MusicServiceConnection, для создания которого нужен context

        @Provides
        fun providesUserAgentInterceptor(
            @ApplicationContext context: Context
        ) =
            UserAgentInterceptor(context) // Создаём экземпляр нашего класса UserAgentInterceptor, для создания которого нужен context

        // Один OkHttpClient на всё приложение: у каждого клиента свой пул соединений и потоки (рекомендация OkHttp).
        // Раньше клиент создавался в RadioServiceWrapper заново для каждого запроса
        @Provides
        @Singleton
        fun providesOkHttpClient(userAgentInterceptor: UserAgentInterceptor): OkHttpClient =
            OkHttpClient.Builder()
                .addInterceptor(userAgentInterceptor)
                // Без явных таймаутов попытка к недоступному серверу длилась ~20 с (по 10 с на IPv6 и IPv4 адрес),
                // и за время полосы загрузки успевали пройти всего 2-3 попытки
                .connectTimeout(NETWORK_CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(NETWORK_READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .callTimeout(
                    NETWORK_CALL_TIMEOUT,
                    TimeUnit.MILLISECONDS
                ) // весь запрос целиком, включая скачивание списка
                .build()
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
    @Binds
    abstract fun bindsFavouriteListInteractor(
        favouriteListInteractor: FavouriteListUseCase
    ): IFavouriteListUseCase

    @Binds
    abstract fun bindsChangeFavouriteUseCase(
        changeFavouriteUseCase: ChangeFavouriteUseCase
    ): IChangeFavouriteUseCase

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
    abstract fun bindsMainRadioUseCase(
        mainRadioInteractor: MainRadioUseCase
    ): IMainRadioUseCase

    @Binds
    abstract fun bindsAuthRepository(
        authRepository: AuthRepository
    ): IAuthRepository

    @Binds
    abstract fun bindsFavoriteStationRepository(
        favoriteStationRepository: FavoriteStationRepository
    ): IFavoriteStationRepository

    @Binds
    abstract fun bindsMainRadioStationRepository(
        mainRadioStationRepository: MainRadioStationRepository
    ): IMainRadioStationRepository

    @Binds
    abstract fun bindsLocalUserDataSource(
        localUserDataSource: LocalUserDataSource
    ): ILocalUserDataSource

    @Binds
    abstract fun bindsLocalFavoriteDataSource(
        localFavoriteDataSource: LocalFavoriteDataSource
    ): ILocalFavoriteDataSource
}