package com.persidius.eos.aurora.database.dao

import androidx.room.*
import com.persidius.eos.aurora.database.entities.Loc
import io.reactivex.Completable
import io.reactivex.Maybe

@Dao
interface LocDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(locs: List<Loc>): Completable

    @Query("DELETE FROM loc")
    fun deleteAll(): Completable

    @Query("SELECT * FROM loc WHERE uatId IN (:uatIds)")
    fun getByUatIds(uatIds: List<Int>): Maybe<List<Loc>>
}