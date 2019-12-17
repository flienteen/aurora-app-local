package com.persidius.eos.aurora.database.entities

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/*
        id
        type
        county {
            id
        }
        uat {
            id
        }
        active
        updatedAt
        labels
        comments
 */

@Entity(
    indices = [
        Index(value = ["uatId"], unique = false),
        Index(value = ["active"], unique = false),
        Index(value = ["type"], unique = false)
    ]
)
data class Groups(
    @PrimaryKey @NonNull val id: String,
    val labels: List<String>,
    val comments: String,
    val type: String,
    val updatedAt: Long,
    val active: Boolean,
    val countyId: Int,
    val uatId: Int
)
