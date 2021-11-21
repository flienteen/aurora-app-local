package com.persidius.eos.aurora.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  indices = [
    Index(value = ["tag"], unique = false),
    Index(value = ["createdAt", "tag"], unique = true)
  ]
)
data class Collection(
  @PrimaryKey(autoGenerate = true)
  val id: Int,
  val extId: String,
  val createdAt: Long,
  val posLat: Double,
  val posLng: Double,
  val vehicleLicensePlate: String,
  val tag: String,
  val uploaded: Boolean,
  val countyId: Int?
)
