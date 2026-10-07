package com.myproject.radiojourney.di

import com.myproject.radiojourney.domain.changeFavouriteUseCase.ChangeFavouriteUseCase
import com.myproject.radiojourney.domain.changeFavouriteUseCase.IChangeFavouriteUseCase
import com.myproject.radiojourney.domain.favouriteListUseCase.FavouriteListUseCase
import com.myproject.radiojourney.domain.favouriteListUseCase.IFavouriteListUseCase
import com.myproject.radiojourney.domain.firstScreenLoadingUseCase.ILoginScreenUseCase
import com.myproject.radiojourney.domain.firstScreenLoadingUseCase.LoginScreenUseCase
import com.myproject.radiojourney.domain.homeRadioUseCase.HomeRadioUseCase
import com.myproject.radiojourney.domain.homeRadioUseCase.IHomeRadioUseCase
import com.myproject.radiojourney.domain.mainRadioUseCase.IMainRadioUseCase
import com.myproject.radiojourney.domain.mainRadioUseCase.MainRadioUseCase
import com.myproject.radiojourney.domain.myStationsUseCase.IMyStationsUseCase
import com.myproject.radiojourney.domain.myStationsUseCase.MyStationsUseCase
import com.myproject.radiojourney.domain.radioListUseCase.IRadioListUseCase
import com.myproject.radiojourney.domain.radioListUseCase.RadioListUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Domain layer: use cases.
 *
 * SingletonComponent, а не ViewModelComponent: use case нужны не только ViewModel, но и плееру
 * (RadioPlaylistSource берёт избранное через FavouriteListUseCase), а он живёт в сервисе, где привязки
 * ViewModelComponent не видны. Привязки из SingletonComponent видны везде. Use case не хранят состояния,
 * а без @Singleton Hilt и так создаёт новый объект при каждом внедрении - поэтому от переноса ничего не меняется
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {

    @Binds
    abstract fun bindsChangeFavouriteUseCase(changeFavouriteUseCase: ChangeFavouriteUseCase): IChangeFavouriteUseCase

    @Binds
    abstract fun bindsFavouriteListUseCase(favouriteListUseCase: FavouriteListUseCase): IFavouriteListUseCase

    @Binds
    abstract fun bindsLoginScreenUseCase(loginScreenUseCase: LoginScreenUseCase): ILoginScreenUseCase

    @Binds
    abstract fun bindsHomeRadioUseCase(homeRadioUseCase: HomeRadioUseCase): IHomeRadioUseCase

    @Binds
    abstract fun bindsRadioListUseCase(radioListUseCase: RadioListUseCase): IRadioListUseCase

    @Binds
    abstract fun bindsMainRadioUseCase(mainRadioUseCase: MainRadioUseCase): IMainRadioUseCase

    @Binds
    abstract fun bindsMyStationsUseCase(myStationsUseCase: MyStationsUseCase): IMyStationsUseCase
}