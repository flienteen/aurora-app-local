package com.persidius.eos.aurora.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.persidius.eos.aurora.database.entities.Group
import io.reactivex.Completable
import io.reactivex.Maybe

@Dao
interface GroupsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(groups: List<Group>): Completable

    @Query("DELETE FROM `Group`")
    fun deleteAll(): Completable

    @Query("SELECT MAX(id) AS result FROM `Group`")
    fun maxId(): Maybe<Int>

    @Query("SELECT COUNT(id) AS result FROM `Group`")
    fun getCount(): Maybe<Int>

    @Query("SELECT * FROM `Group` JOIN GroupsFTS ON (GroupsFTS.rowid = `Group`.rowid) WHERE GroupsFTS MATCH :term")
    fun search(term: String): Maybe<List<Group>>
}
