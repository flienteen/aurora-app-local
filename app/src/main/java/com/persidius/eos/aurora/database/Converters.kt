package com.persidius.eos.aurora.database

import android.util.Log
import androidx.room.TypeConverter
import com.persidius.eos.aurora.util.ArrayOp

// use unicode maths symbols as separators
const val LEVEL_1_SEPARATOR = "∥"
const val LEVEL_2_SEPARATOR = "⊕"

class Converters {
    @TypeConverter
    fun stringFromStringList(value: List<String>): String {
        return value.joinToString(LEVEL_1_SEPARATOR)
    }

    @TypeConverter
    fun stringListfromString(value: String): List<String> {
        return value.split(LEVEL_1_SEPARATOR)
    }

    @TypeConverter
    fun stringFromLabelEdit(value: List<Pair<ArrayOp, String>>): String {
        return value.joinToString(separator = LEVEL_1_SEPARATOR, transform = { p -> "${p.first.name}$LEVEL_2_SEPARATOR${p.second}" })
    }

    @TypeConverter
    fun LabelEditFromString(value: String): List<Pair<ArrayOp, String>> {
        if(value.isEmpty()) {
            return listOf()
        }

        return value.split(LEVEL_1_SEPARATOR).map { part ->
            val parts = part.split(LEVEL_2_SEPARATOR)
            Log.d("DB", part + parts.joinToString { "|" })
            Pair(ArrayOp.valueOf(parts[0]), parts[1])
        }
    }

    @TypeConverter
    fun stringFromTagEdit(value: List<Triple<ArrayOp, Int, String>>): String {
        return value.joinToString(separator = LEVEL_1_SEPARATOR, transform = {
            t -> "${t.first.name}$LEVEL_2_SEPARATOR${t.second}$LEVEL_2_SEPARATOR${t.third}"
        })
    }

    @TypeConverter
    fun tagEditFromString(value: String): List<Triple<ArrayOp, Int, String>> {
        if(value.isEmpty()) {
            return listOf()
        }

        return value.split(LEVEL_1_SEPARATOR).map { part ->
            val parts = part.split(LEVEL_2_SEPARATOR)
            Triple(ArrayOp.valueOf(parts[0]), parts[1].toInt(10), parts[2])
        }
    }
}