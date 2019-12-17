package com.persidius.eos.aurora.database.dao

import androidx.room.*
import com.persidius.eos.aurora.database.entities.Artery
import io.reactivex.Completable
import io.reactivex.Maybe


@Dao
interface ArteryDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(arteries: List<Artery>): Completable

    @Query("DELETE FROM artery")
    fun deleteAll(): Completable

    @Query("SELECT * FROM artery WHERE locId IN (:locIds)")
    fun getByLocIds(locIds: List<Int>): Maybe<List<Artery>>
}
