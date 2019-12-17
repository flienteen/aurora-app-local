package com.persidius.eos.aurora.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(foreignKeys = [
    ForeignKey(
        entity = County::class,
        parentColumns = ["id"],
        childColumns = ["countyId"],
        onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["countyId"], unique = false)
    ]
)
data class Uat(
    @PrimaryKey val id: Int,
    val name: String,
    val countyId: Int
)