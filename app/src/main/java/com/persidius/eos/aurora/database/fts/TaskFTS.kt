package com.persidius.eos.aurora.database.fts

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import com.persidius.eos.aurora.database.entities.Task

@Fts4(contentEntity = Task::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(
    tableName = "TaskFTS"
)
data class TaskFTS(
    val id: Int,
    val status: String,
    val groups: List<String>,
    val users: List<String>,
    val recipients: List<String>
)