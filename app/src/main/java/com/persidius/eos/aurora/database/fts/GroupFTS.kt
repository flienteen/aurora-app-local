package com.persidius.eos.aurora.database.fts

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import com.persidius.eos.aurora.database.entities.Group

@Fts4(contentEntity = Group::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(
    tableName = "GroupFTS"
)
data class GroupFTS(
    val eosId: String
)