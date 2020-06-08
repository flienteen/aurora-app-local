package com.persidius.eos.aurora.database.fts

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import com.persidius.eos.aurora.database.entities.Recipient

@Fts4(contentEntity = Recipient::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(
    tableName = "RecipientFTS"
)
data class RecipientFTS(
    val eosId: String,
    val addressStreet: String,
    val addressNumber: String
)