package com.myproject.radiojourney.data.localDatabaseRoom

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng

import com.google.gson.reflect.TypeToken

import com.google.gson.Gson
import java.lang.reflect.Type


/**
 * Конвертер для LatLng
 */
object LatLngConverter {

//    @TypeConverter
//    fun stringToModel(json: String?): LatLng? {
//        val gson = Gson()
//        val type: Type = object : TypeToken<LatLng?>() {}.type
//        return gson.fromJson(json, type)
//    }
//
//    @TypeConverter
//    fun modelToString(position: LatLng?): String? {
//        val gson = Gson()
//        val type: Type = object : TypeToken<LatLng?>() {}.type
//        return gson.toJson(position, type)
//    }

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