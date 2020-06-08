package com.persidius.eos.aurora.database.entities

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [
        Index(value = ["uatId"], unique = false),
        Index(value = ["active"], unique = false),
        Index(value = ["type"], unique = false),
        Index(value = ["updatedAt"], unique = false)
    ]
)
data class Groups(
    @PrimaryKey
    @NonNull
    val eosId: String,

    val id: Int,

    val labels: Map<String, String?>,
    val comments: String,
    val type: String,
    val addressStreet: String,
    val addressNumber: String,

    val locId: Int,
    val countyId: Int,
    val uatId: Int
)
