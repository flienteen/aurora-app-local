package com.persidius.eos.aurora.database.fts

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import com.persidius.eos.aurora.database.entities.Groups

@Fts4(contentEntity = Groups::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(
    tableName = "GroupsFTS"
)
data class GroupsFTS(
    val id: String
)