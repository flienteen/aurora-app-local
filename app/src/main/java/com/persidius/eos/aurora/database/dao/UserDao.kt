package com.persidius.eos.aurora.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.persidius.eos.aurora.database.LongQueryResult
import com.persidius.eos.aurora.database.entities.User
import io.reactivex.Completable
import io.reactivex.Maybe


@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(users: List<User>): Completable

    @Query("DELETE FROM User")
    fun deleteAll(): Completable

    @Query("SELECT MAX(updatedAt) AS result FROM User")
    fun lastUpdatedAt(): Maybe<LongQueryResult>

    @Query("SELECT COUNT(id) AS result FROM User")
    fun getCount(): Maybe<LongQueryResult>
}