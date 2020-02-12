package com.persidius.eos.aurora.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.persidius.eos.aurora.database.entities.TaskPatch
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
interface TaskPatchDao {
    @Insert
    fun insert(p: TaskPatch): Single<Long>

    @Query("SELECT * FROM TaskPatch WHERE id = :id")
    fun getById(id: Int): Maybe<TaskPatch>

    @Query("SELECT * FROM TaskPatch WHERE sessionId = :sessionId")
    fun getBySessionId(sessionId: Int): Maybe<List<TaskPatch>>
}
