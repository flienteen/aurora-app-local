package com.persidius.eos.aurora.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Vehicle(
  @PrimaryKey(autoGenerate = true)
  val id: Int,
  val vehicleLicensePlate: String,
  val countyId: Int,
)
