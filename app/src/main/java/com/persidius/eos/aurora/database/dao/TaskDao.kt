package com.persidius.eos.aurora.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.persidius.eos.aurora.database.LongQueryResult
import com.persidius.eos.aurora.database.entities.Task
import io.reactivex.Completable
import io.reactivex.Maybe

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tasks: List<Task>): Completable

    @Query("DELETE FROM Task")
    fun deleteAll(): Completable

    @Query("SELECT * FROM Task WHERE id = :taskId")
    fun getById(taskId: Int): Maybe<Task>

    @Query("SELECT MAX(updatedAt) AS result FROM Task LIMIT 1")
    fun lastUpdatedAt(): Maybe<LongQueryResult>

    @Query("SELECT COUNT(id) AS result FROM Task")
    fun getCount(): Maybe<LongQueryResult>

    @Query("SELECT * FROM Task JOIN TaskFTS ON (TaskFTS.rowid = Task.rowid) WHERE TaskFTS MATCH :term")
    fun search(term: String): Maybe<List<Task>>

    @Query("SELECT * FROM Task WHERE posLat>(:posLat-:radius) AND posLat<(:posLat+:radius) AND posLng>(:posLng-:radius) AND posLng<(:posLng+:radius)")
    fun searchByLocation(posLat: Double, posLng: Double, radius: Double): Maybe<List<Task>>
}
