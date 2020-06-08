package com.persidius.eos.aurora.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    companion object {
        private val gson = Gson()
        private val stringMapType = object: TypeToken<Map<String, String?>>() { }.type
    }

    @TypeConverter
    fun stringMapToJson(value: Map<String, String?>?): String? = if(value == null) null else gson.toJson(value)

    @TypeConverter
    fun jsonToStringMap(value: String?): Map<String, String?>? = if(value == null) null else gson.fromJson(value, stringMapType)
}