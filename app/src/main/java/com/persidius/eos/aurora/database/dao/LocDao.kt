package com.persidius.eos.aurora.database.dao

import androidx.room.*
import com.persidius.eos.aurora.database.entities.Loc
import io.reactivex.Completable
import io.reactivex.Maybe

@Dao
interface LocDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun rxInsert(locs: List<Loc>): Completable

  @Query("DELETE FROM Loc")
  fun rxDeleteAll(): Completable

  @Query("SELECT * FROM Loc")
  fun getAll(): Maybe<List<Loc>>

  @Query("SELECT * FROM Loc WHERE uatId IN (:uatIds)")
  fun getByUatIds(uatIds: List<Int>): Maybe<List<Loc>>

  @Query("SELECT * FROM Loc WHERE id IN (:ids)")
  fun getByIds(ids: List<Int>): Maybe<List<Loc>>

  @Query("SELECT COUNT(id) AS result FROM Loc")
  fun getCount(): Maybe<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(locs: List<Loc>)

  @Query("DELETE FROM Loc")
  suspend fun deleteAll()
}