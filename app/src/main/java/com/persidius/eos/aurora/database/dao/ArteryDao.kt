package com.persidius.eos.aurora.database.dao

import androidx.room.*
import com.persidius.eos.aurora.database.LongQueryResult
import com.persidius.eos.aurora.database.entities.Artery
import io.reactivex.Completable
import io.reactivex.Maybe


@Dao
interface ArteryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(arteries: List<Artery>): Completable

    @Query("DELETE FROM Artery")
    fun deleteAll(): Completable

    @Query("SELECT * FROM Artery WHERE locId IN (:locIds)")
    fun getByLocIds(locIds: List<Int>): Maybe<List<Artery>>

    @Query("SELECT COUNT(id) AS result FROM Artery")
    fun getCount(): Maybe<LongQueryResult>

    @Query("SELECT * FROM Artery JOIN ArteryFTS ON (ArteryFTS.rowid = Artery.rowid) WHERE locId = :locId AND ArteryFTS MATCH :term")
    fun search(locId: Int, term: String): Maybe<List<Artery>>

}
