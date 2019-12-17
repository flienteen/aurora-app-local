package com.persidius.eos.aurora.database.entities

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    indices = [
        Index(value = ["recipientId"], unique = false)
    ]
)
data class RecipientTag(
    @PrimaryKey
    @NonNull val tag: String,
    @NonNull val recipientId: String,
    @NonNull val slot: Int
)