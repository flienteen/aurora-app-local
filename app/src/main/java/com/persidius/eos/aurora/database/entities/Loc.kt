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
        onDelete = ForeignKey.SET_NULL
    ),
    ForeignKey(
        entity = County::class,
        parentColumns = ["id"],
        childColumns = ["countyId"],
        onDelete = ForeignKey.SET_NULL
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