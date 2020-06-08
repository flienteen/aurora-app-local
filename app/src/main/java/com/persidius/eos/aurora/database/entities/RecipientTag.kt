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
    val slot: Int
)