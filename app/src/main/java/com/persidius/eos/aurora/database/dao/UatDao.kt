package com.persidius.eos.aurora.database.dao

import androidx.room.*
import com.persidius.eos.aurora.database.LongQueryResult
import com.persidius.eos.aurora.database.entities.Uat
import io.reactivex.Completable
import io.reactivex.Maybe

@Dao
interface UatDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(uats: List<Uat>): Completable

    @Query("DELETE FROM Uat")
    fun deleteAll(): Completable

    @Query("SELECT * FROM Uat")
    fun getAll(): Maybe<List<Uat>>

    @Query("SELECT * FROM Uat WHERE countyId IN (:countyIds)")
    fun getByCountyIds(countyIds: List<Int>): Maybe<List<Uat>>

    @Query("SELECT * FROM Uat WHERE id IN (:ids)")
    fun getByIds(ids: List<Int>): Maybe<List<Uat>>

    @Query("SELECT COUNT(id) AS result FROM Uat")
    fun getCount(): Maybe<LongQueryResult>

    @Query("SELECT * FROM Uat JOIN UatFTS ON (UatFTS.rowid = Uat.rowid) WHERE countyId IN (:countyIds) AND UatFTS MATCH :term")
    fun searchByCounty(countyIds: List<Int>, term: String): Maybe<List<Uat>>

}
