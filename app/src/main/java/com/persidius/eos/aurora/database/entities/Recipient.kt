package com.persidius.eos.aurora.database.entities

import androidx.annotation.NonNull
import androidx.room.*

@Entity(
    indices = [
        Index(value = ["countyId"], unique = false),
        Index(value = ["uatId"], unique = false),
        Index(value = ["locId"], unique = false),
        Index(value = ["updatedAt"], unique = false)
    ]
)
data class Recipient(
    @PrimaryKey
    @NonNull val id: String,
    val addressNumber: String,
    val addressStreet: String,
    val posLat: Double,
    val posLng: Double,

    val labels: List<String>,
    val comments: String,
    val active: Boolean,
    val updatedAt: Long,
    val groupId: String?,

    val stream: String,
    val size: String,

    val locId: Int,
    val uatId: Int,
    val countyId: Int
)