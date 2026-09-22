package com.myproject.radiojourney.data.dataSource.local.country

import android.content.Context
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

/**
 * В какой стране телефон - без разрешения на местоположение.
 *
 * Плейлист при первом запуске раньше всегда был Антарктидой (AQ). Узнать страну пользователя через местоположение
 * можно, но только после разрешения: пока его не дали (или если отказали), страны нет. А код страны сотовой сети
 * и SIM-карты Android отдаёт любому приложению без разрешений - по нему и выбираем страну первого плейлиста
 */
class DeviceCountryDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Коды стран, которые знает телефон, от самого точного к запасному ("RU", "DE"...). Может быть пустым
    fun getCountryCodeCandidates(): List<String> {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        return listOfNotNull(
            telephonyManager?.networkCountryIso, // страна сотовой сети, в которой телефон сейчас
            telephonyManager?.simCountryIso, // страна оператора SIM-карты (у планшета без SIM - пусто)
            Locale.getDefault().country // регион из настроек телефона ("Россия" в "Язык и регион")
        )
            .map { it.uppercase(Locale.ROOT) }
            .filter { it.length == 2 } // пустые и непонятные значения пропускаем
            .distinct()
    }
}