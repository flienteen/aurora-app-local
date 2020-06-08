package com.persidius.eos.aurora.database.entities

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [
        Index(value = ["uatId"], unique = false),
        Index(value = ["type"], unique = false)
    ]
)
data class Group(
    @PrimaryKey
    @NonNull
    val eosId: String,

    val id: Int,
    val type: String,

    val locId: Int,
    val countyId: Int,
    val uatId: Int
)
