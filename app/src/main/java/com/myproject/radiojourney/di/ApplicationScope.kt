package com.myproject.radiojourney.di

import javax.inject.Qualifier

/**
 * Корутинный scope приложения: живёт, пока живёт процесс.
 *
 * Нужен для работы, которая не должна отменяться вместе с экраном. Пример - выход из аккаунта:
 * пользователь нажимает "выйти", экран сразу закрывается, а запись в DataStore не успевает дойти до диска.
 * В viewModelScope такая корутина отменилась бы вместе с ViewModel (рекомендация developer.android.com)
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope