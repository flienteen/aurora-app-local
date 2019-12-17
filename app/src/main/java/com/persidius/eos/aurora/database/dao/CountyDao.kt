package com.persidius.eos.aurora.database.dao

import androidx.room.*
import com.persidius.eos.aurora.database.entities.County
import io.reactivex.Completable
import io.reactivex.Maybe

@Dao
interface CountyDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(counties: List<County>): Completable

    @Query("DELETE FROM county")
    fun deleteAll(): Completable

    @Query("SELECT * FROM county WHERE id IN (:ids)")
    fun getByIds(ids: IntArray): Maybe<List<County>>

    @Query("SELECT * FROM county")
    fun getAll(): Maybe<List<County>>
}