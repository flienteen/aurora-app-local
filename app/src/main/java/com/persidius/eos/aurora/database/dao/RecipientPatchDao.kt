package com.persidius.eos.aurora.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.persidius.eos.aurora.database.entities.RecipientPatch
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
interface RecipientPatchDao {
    @Insert
    fun insert(p: RecipientPatch): Single<Long>

    @Query("SELECT * FROM RecipientPatch WHERE id = :id")
    fun getById(id: Int): Maybe<RecipientPatch>

    @Query("SELECT * FROM RecipientPatch WHERE sessionId = :sessionId")
    fun getBySessionId(sessionId: Int): Maybe<List<RecipientPatch>>
}
