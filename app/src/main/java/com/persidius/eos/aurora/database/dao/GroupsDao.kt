package com.persidius.eos.aurora.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.persidius.eos.aurora.database.LongQueryResult
import com.persidius.eos.aurora.database.entities.Groups
import io.reactivex.Completable
import io.reactivex.Maybe

@Dao
interface GroupsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(groups: List<Groups>): Completable

    @Query("DELETE FROM Groups")
    fun deleteAll(): Completable

    @Query("SELECT MAX(updatedAt) AS result FROM Groups")
    fun lastUpdatedAt(): Maybe<LongQueryResult>

    @Query("SELECT COUNT(id) AS result FROM Groups")
    fun getCount(): Maybe<LongQueryResult>
}
