package com.persidius.eos.aurora.database.dao

import androidx.room.*
import com.persidius.eos.aurora.database.LongQueryResult
import com.persidius.eos.aurora.database.entities.County
import io.reactivex.Completable
import io.reactivex.Maybe

@Dao
interface CountyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(counties: List<County>): Completable

    @Query("DELETE FROM County")
    fun deleteAll(): Completable

    @Query("SELECT * FROM County WHERE id IN (:ids)")
    fun getByIds(ids: IntArray): Maybe<List<County>>

    @Query("SELECT * FROM County")
    fun getAll(): Maybe<List<County>>

    @Query("SELECT COUNT(id) AS result FROM County")
    fun getCount(): Maybe<LongQueryResult>
}