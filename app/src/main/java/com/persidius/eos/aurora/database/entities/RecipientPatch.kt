package com.persidius.eos.aurora.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.persidius.eos.aurora.util.ArrayOp

@Entity(
    indices = [
        Index(value = ["sessionId"], unique = false),
        Index(value = ["recipientId"], unique = false)
    ]
)
data class RecipientPatch(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val recipientId: String,
    val sessionId: Long?,

    val createdAt: Long,
    val createdBy: String,

    val posLat: Double?,
    val posLng: Double?,

    val stream: String?,
    val size: String?,

    val addressStreet: String?,
    val addressNumber: String?,

    val uatId: Int?,
    val locId: Int?,

    val comments: String?,
    val active: Boolean?,

    val labels: List<Pair<ArrayOp, String>>,
    val tags: List<Triple<ArrayOp, Int, String>>,
    val groupId: String?
)