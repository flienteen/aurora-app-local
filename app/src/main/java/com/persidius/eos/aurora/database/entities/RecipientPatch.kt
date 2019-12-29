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

    val posLat: Float?,
    val posLng: Float?,

    val stream: String?,
    val size: String?,

    val addressString: String?,
    val addressNumber: String?,

    val uatId: Int?,
    val locId: Int?,

    val comments: String?,
    val active: Boolean?,

    // TODO: Implement label add/remove format as well as tag add/remove fmt.
    val labels: List<Pair<ArrayOp, String>>,
    val tags: List<Triple<ArrayOp, Int, String>>,
    val groupId: String?
)