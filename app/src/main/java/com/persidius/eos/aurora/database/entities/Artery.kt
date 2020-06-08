package com.persidius.eos.aurora.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(foreignKeys = [
    ForeignKey(
        entity = Loc::class,
        parentColumns = ["id"],
        childColumns = ["locId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(value = ["locId"], unique = false)
    ]
)
data class Artery(
    @PrimaryKey val id: Int,
    val name: String,
    val prefix: String,
    val locId: Int
)