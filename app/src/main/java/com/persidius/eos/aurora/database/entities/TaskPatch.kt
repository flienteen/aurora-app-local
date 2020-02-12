package com.persidius.eos.aurora.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.persidius.eos.aurora.util.ArrayOp

@Entity(
    indices = [
        Index(value = ["sessionId"], unique = false),
        Index(value = ["taskId"], unique = false)
    ]
)
data class TaskPatch(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val gid: String?,
    val taskId: Int,
    val updatedAt: Int,
    val sessionId: Long?,
    val comments: String?,
    val recipients: List<String>?
)