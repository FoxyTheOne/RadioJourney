package com.myproject.radiojourney.data.localDatabaseRoom

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng

/**
 * Конвертер для LatLng
 */
object LatLngConverter {
    private const val ENTRY_SEPARATOR = "||"

    @TypeConverter
    fun latLngToString(latLng: LatLng): String {
        return "${latLng.latitude}$ENTRY_SEPARATOR${latLng.longitude}"
    }

    @TypeConverter
    fun stringToLatLng(string: String): LatLng {
        val latLngArray = string.split(ENTRY_SEPARATOR)

        return LatLng(latLngArray[0].toDouble(), latLngArray[1].toDouble())
    }
}