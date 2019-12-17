package com.persidius.eos.aurora.database.entities

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class RecommendedLabel(
    @PrimaryKey
    @NonNull val label: String,
    @NonNull val displayName: String
)