package com.persidius.eos.aurora.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Completable
import java.security.acl.Group

@Dao
interface GroupsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(groups: List<Group>): Completable

    @Query("DELETE FROM groups")
    fun deleteAll(): Completable
}
