package com.myproject.radiojourney.presentation.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Есть ли подключение к интернету.
 * Раньше - MainViewModel.isInternetAvailable(context): ViewModel не должна работать с Context экрана.
 * Ветка для Android до 6.0 убрана: minSdk приложения 23
 */
fun Context.isInternetAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
}
