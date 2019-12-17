package com.persidius.eos.aurora.database.entities

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [
        Index(value = ["countyId"], unique = false),
        Index(value = ["uatId"], unique = false),
        Index(value = ["locId"], unique = false)
    ]
)
data class Recipient(
    @PrimaryKey @NonNull val id: String,
    val addressNumber: String,
    val addressStreet: String,
    val posLat: Double,
    val posLng: Double,

    val labels: List<String>,
    val comments: String,
    val active: Boolean,
    val updatedAt: Long,


    val locId: Int,
    val uatId: Int,
    val countyId: Int
)