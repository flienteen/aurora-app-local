package com.persidius.eos.aurora.database.fts

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import com.persidius.eos.aurora.database.entities.Artery

@Fts4(contentEntity = Artery::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(
    tableName = "ArteryFTS"
)
data class ArteryFTS(
    val prefix: String,
    val name: String
)
