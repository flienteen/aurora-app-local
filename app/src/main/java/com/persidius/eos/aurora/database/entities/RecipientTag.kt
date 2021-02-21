package com.persidius.eos.aurora.database.entities

import androidx.annotation.NonNull
import androidx.room.*
import javax.annotation.Nullable

@Entity(
    indices = [
        Index(value = ["tag"], unique = true),
        Index(value = ["recipientId"], unique = false)
    ]
)
data class RecipientTag(
    @PrimaryKey
    @NonNull val tag: String,

    // Internal, record ID
    val id: Int,

    val recipientId: String?,
    val slot: Int,

    // fromDate is helpful when we want to keep a synced record
    val updatedAt: String? = null
)