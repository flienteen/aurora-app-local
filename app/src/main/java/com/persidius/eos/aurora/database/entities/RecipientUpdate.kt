package com.persidius.eos.aurora.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [
        Index(value = ["eosId"], unique = false)
    ]
)
data class RecipientPatch(
    @PrimaryKey(autoGenerate = true)
    val updateId: Int,

    val eosId: String,

    // Internal. For audit purposes.
    val createdAt: Long,
    // Internal. For audit purposes.
    val createdBy: String,
    // Internal. For audit purposes.
    val id: Int,

    val posLat: Double?,
    val posLng: Double?,

    val stream: String?,
    val size: String?,

    val addressStreet: String?,
    val addressNumber: String?,

    val uatId: Int?,
    val locId: Int?,

    val comments: String?,

    val labels: Map<String, String?>,

    val tag0: String?,
    val tag1: String?,

    val groupId: String?
)