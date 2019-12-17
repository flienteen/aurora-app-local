package com.persidius.eos.aurora.database.dao

import androidx.room.*
import com.persidius.eos.aurora.database.entities.Uat
import io.reactivex.Completable
import io.reactivex.Maybe

@Dao
interface UatDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(uats: List<Uat>): Completable

    @Query("DELETE FROM uat")
    fun deleteAll(): Completable

    @Query("SELECT * FROM uat")
    fun getAll(): Maybe<List<Uat>>

    @Query("SELECT * FROM uat WHERE countyId IN (:countyIds)")
    fun getByCountyIds(countyIds: List<Int>): Maybe<List<Uat>>
}
