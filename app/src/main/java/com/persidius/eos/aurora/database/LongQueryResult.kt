package com.persidius.eos.aurora.database

import androidx.room.ColumnInfo

data class LongQueryResult(
    @ColumnInfo(name = "result")
    val result: Long
)
