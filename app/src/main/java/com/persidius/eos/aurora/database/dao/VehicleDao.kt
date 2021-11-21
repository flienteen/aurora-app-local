package com.persidius.eos.aurora.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.persidius.eos.aurora.database.entities.Vehicle
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface VehicleDao {
  @Insert
  suspend fun insert(vehicles: List<Vehicle>)

  @Query("DELETE FROM Vehicle")
  suspend fun deleteAll()

  @Query("SELECT * FROM Vehicle where vehicleLicensePlate = :vehicleLicensePlate")
  suspend fun find(vehicleLicensePlate: String): List<Vehicle>

  @Query("SELECT * FROM Vehicle")
  fun rxFindAll(): Single<List<Vehicle>>
}