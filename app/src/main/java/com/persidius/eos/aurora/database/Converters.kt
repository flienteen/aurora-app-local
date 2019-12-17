package com.persidius.eos.aurora.database

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString("|")
    }

    @TypeConverter
    fun fromString(value: String): List<String> {
        return value.split("|")
    }
}