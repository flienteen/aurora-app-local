package com.persidius.eos.aurora.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class County (
    @PrimaryKey val id: Int,
    val seq: Int,
    val short_: String,
    val name: String
)

