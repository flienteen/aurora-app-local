package com.persidius.eos.aurora.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(foreignKeys = [
    ForeignKey(
        entity = Uat::class,
        parentColumns = ["id"],
        childColumns = ["uatId"],
        onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
        entity = County::class,
        parentColumns = ["id"],
        childColumns = ["countyId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(unique = false, value = ["uatId"]),
        Index(unique = false, value = ["countyId"])
    ]
)
data class Loc(
    @PrimaryKey val id: Int,
    val name: String,
    val uatId: Int,
    val countyId: Int
)