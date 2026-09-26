package com.myproject.radiojourney.di

import android.content.Context
import androidx.room.Room
import com.myproject.radiojourney.data.dataSource.local.auth.ILocalUserDataSource
import com.myproject.radiojourney.data.dataSource.local.auth.LocalUserDataSource
import com.myproject.radiojourney.data.dataSource.local.favorite.ILocalFavoriteDataSource
import com.myproject.radiojourney.data.dataSource.local.favorite.LocalFavoriteDataSource
import com.myproject.radiojourney.data.dataSource.local.myStation.ILocalMyStationDataSource
import com.myproject.radiojourney.data.dataSource.local.myStation.LocalMyStationDataSource
import com.myproject.radiojourney.data.dataSource.local.radio.ILocalRadioDataSource
import com.myproject.radiojourney.data.dataSource.local.radio.LocalRadioDataSource
import com.myproject.radiojourney.data.dataSource.network.INetworkRadioDataSource
import com.myproject.radiojourney.data.dataSource.network.NetworkRadioDataSource
import com.myproject.radiojourney.data.dataSource.network.service.IRadioServiceWrapper
import com.myproject.radiojourney.data.dataSource.network.service.RadioServiceWrapper
import com.myproject.radiojourney.data.dataSource.network.service.UserAgentInterceptor
import com.myproject.radiojourney.data.localDatabaseRoom.AppRoomDBAbstract
import com.myproject.radiojourney.data.localDatabaseRoom.ICountryDAO
import com.myproject.radiojourney.data.localDatabaseRoom.IMyStationDAO
import com.myproject.radiojourney.data.localDatabaseRoom.IRadioStationDAO
import com.myproject.radiojourney.data.localDatabaseRoom.ISavedStationDAO
import com.myproject.radiojourney.data.localDatabaseRoom.MIGRATION_12_13
import com.myproject.radiojourney.data.localDatabaseRoom.MIGRATION_13_14
import com.myproject.radiojourney.data.preference.AppPreferenceStorage
import com.myproject.radiojourney.data.preference.IAppPreferenceStorage
import com.myproject.radiojourney.data.repository.AuthRepository
import com.myproject.radiojourney.data.repository.FavoriteStationRepository
import com.myproject.radiojourney.data.repository.MainRadioStationRepository
import com.myproject.radiojourney.data.repository.MyStationRepository
import com.myproject.radiojourney.domain.changeFavouriteUseCase.ChangeFavouriteUseCase
import com.myproject.radiojourney.domain.changeFavouriteUseCase.IChangeFavouriteUseCase
import com.myproject.radiojourney.domain.favouriteListUseCase.FavouriteListUseCase
import com.myproject.radiojourney.domain.favouriteListUseCase.IFavouriteListUseCase
import com.myproject.radiojourney.domain.firstScreenLoadingUseCase.ILoginScreenUseCase
import com.myproject.radiojourney.domain.firstScreenLoadingUseCase.LoginScreenUseCase
import com.myproject.radiojourney.domain.homeRadioUseCase.HomeRadioUseCase
import com.myproject.radiojourney.domain.homeRadioUseCase.IHomeRadioUseCase
import com.myproject.radiojourney.domain.iRepository.IAuthRepository
import com.myproject.radiojourney.domain.iRepository.IFavoriteStationRepository
import com.myproject.radiojourney.domain.iRepository.IMainRadioStationRepository
import com.myproject.radiojourney.domain.iRepository.IMyStationRepository
import com.myproject.radiojourney.domain.mainRadioUseCase.IMainRadioUseCase
import com.myproject.radiojourney.domain.mainRadioUseCase.MainRadioUseCase
import com.myproject.radiojourney.domain.myStationsUseCase.IMyStationsUseCase
import com.myproject.radiojourney.domain.myStationsUseCase.MyStationsUseCase
import com.myproject.radiojourney.domain.radioListUseCase.IRadioListUseCase
import com.myproject.radiojourney.domain.radioListUseCase.RadioListUseCase
import com.myproject.radiojourney.other.Constants.NETWORK_CALL_TIMEOUT
import com.myproject.radiojourney.other.Constants.NETWORK_CONNECT_TIMEOUT
import com.myproject.radiojourney.other.Constants.NETWORK_READ_TIMEOUT
import com.myproject.radiojourney.utils.exoplayer.MusicServiceConnection
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

/**
 * Data layer: база данных, сеть, источники данных и репозитории.
 * В SingletonComponent: ими пользуются не только ViewModel, но и сервис плеера (RadioPlaylistSource) и WorkManager.
 * Раньше репозитории были в ViewModelComponent, и сервис обращался к DAO и NetworkRadioDataSource напрямую, в обход репозиториев
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    companion object {
        // ROOM -> 1. База данных Room
        @Provides
        @Singleton
        fun providesAppDatabase(@ApplicationContext appContext: Context): AppRoomDBAbstract =
            Room.databaseBuilder(appContext, AppRoomDBAbstract::class.java, "AppRoomDatabase")
                // Миграция нужна, чтобы у тех, кто уже пользуется приложением, не пропали избранные станции (см. DatabaseMigrations)
                .addMigrations(MIGRATION_12_13, MIGRATION_13_14)
                .build()

        @Provides
        fun providesCountryDAO(appDatabase: AppRoomDBAbstract): ICountryDAO =
            appDatabase.getCountryDAO()

        @Provides
        fun providesRadioStationDAO(appDatabase: AppRoomDBAbstract): IRadioStationDAO =
            appDatabase.getRadioStationDAO()

        @Provides
        fun providesMyStationDAO(appDatabase: AppRoomDBAbstract): IMyStationDAO =
            appDatabase.getMyStationDAO()

        @Provides
        fun providesSavedStationDAO(appDatabase: AppRoomDBAbstract): ISavedStationDAO =
            appDatabase.getSavedStationDAO()

        @Provides
        @Singleton
        fun providesMusicServiceConnection(@ApplicationContext context: Context) =
            MusicServiceConnection(context)

        @Provides
        fun providesUserAgentInterceptor(@ApplicationContext context: Context) =
            UserAgentInterceptor(context)

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
    abstract fun bindsPreferenceStorage(appPreferenceStorage: AppPreferenceStorage): IAppPreferenceStorage

    @Binds
    abstract fun bindRadioServiceWrapper(radioServiceWrapper: RadioServiceWrapper): IRadioServiceWrapper

    @Binds
    abstract fun bindsNetworkRadioDataSource(networkRadioDataSource: NetworkRadioDataSource): INetworkRadioDataSource

    @Binds
    abstract fun bindsLocalRadioDataSource(localRadioDataSource: LocalRadioDataSource): ILocalRadioDataSource

    @Binds
    abstract fun bindsLocalFavoriteDataSource(localFavoriteDataSource: LocalFavoriteDataSource): ILocalFavoriteDataSource

    @Binds
    abstract fun bindsLocalMyStationDataSource(localMyStationDataSource: LocalMyStationDataSource): ILocalMyStationDataSource

    @Binds
    abstract fun bindsLocalUserDataSource(localUserDataSource: LocalUserDataSource): ILocalUserDataSource

    @Binds
    abstract fun bindsMainRadioStationRepository(mainRadioStationRepository: MainRadioStationRepository): IMainRadioStationRepository

    @Binds
    abstract fun bindsFavoriteStationRepository(favoriteStationRepository: FavoriteStationRepository): IFavoriteStationRepository

    @Binds
    abstract fun bindsMyStationRepository(myStationRepository: MyStationRepository): IMyStationRepository

    @Binds
    abstract fun bindsAuthRepository(authRepository: AuthRepository): IAuthRepository
}

/**
 * Domain layer: use cases. Нужны только ViewModel
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class DomainModule {

    @Binds
    abstract fun bindsChangeFavouriteUseCase(changeFavouriteUseCase: ChangeFavouriteUseCase): IChangeFavouriteUseCase

    @Binds
    abstract fun bindsFavouriteListInteractor(favouriteListInteractor: FavouriteListUseCase): IFavouriteListUseCase

    @Binds
    abstract fun bindsLoginScreenInteractor(loginScreenInteractor: LoginScreenUseCase): ILoginScreenUseCase

    @Binds
    abstract fun bindsHomeRadioInteractor(homeRadioInteractor: HomeRadioUseCase): IHomeRadioUseCase

    @Binds
    abstract fun bindsRadioListInteractor(radioListInteractor: RadioListUseCase): IRadioListUseCase

    @Binds
    abstract fun bindsMainRadioUseCase(mainRadioInteractor: MainRadioUseCase): IMainRadioUseCase

    @Binds
    abstract fun bindsMyStationsUseCase(myStationsInteractor: MyStationsUseCase): IMyStationsUseCase
}