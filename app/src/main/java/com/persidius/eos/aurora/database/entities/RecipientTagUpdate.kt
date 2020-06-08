package com.persidius.eos.aurora.database.entities;

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [
        Index(value = ["tag"], unique = false),
        Index(value = ["uploaded"], unique = false)
    ]
)
public data class RecipientTagUpdate (
    @PrimaryKey(autoGenerate = true)
    val updateId: Int,

    val tag: String,
    val slot: Int,
    val recipientId: String?,

    // Internal. For audit purposes. (time when this was created. ISO Timestring)
    val createdAt: String,
    // Internal. For audit purposes. (email of creator)
    val createdBy: String,
    // Internal. For audit purposes.
    val id: Int,

    val uploaded: Boolean
)

