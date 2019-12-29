package com.persidius.eos.aurora.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val createdAt: Long,
    val refCount: Int,          // useful for determining when to *CLOSE* the session
    val open: Boolean,          // State is either OPEN or CLOSED.
    val uploaded: Boolean,
    val txId: String? = null          // txId with which this was uploaded, if any
)