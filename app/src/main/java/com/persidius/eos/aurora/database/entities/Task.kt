package com.persidius.eos.aurora.database.entities

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [
        Index(value = ["countyId"], unique = false),
        Index(value = ["uatId"], unique = false),
        Index(value = ["locId"], unique = false),
        Index(value = ["updatedAt"], unique = false)
    ]
)
data class Task(
    @PrimaryKey
    @NonNull val id: Int,
    val gid: String?,
    val assignedTo: String?,
    val validFrom: Long,
    val validTo: Long,
    val updatedBy: String,
    val updatedAt: Long,
    val status: String,
    val posLat: Double?,
    val posLng: Double?,
    val comments: String,
    val uatId: Int,
    val locId: Int,
    val countyId: Int,
    val groups: List<String>,
    val users: List<String>,
    val recipients: List<String>
)