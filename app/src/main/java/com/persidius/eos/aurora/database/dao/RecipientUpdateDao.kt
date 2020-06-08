package com.persidius.eos.aurora.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.persidius.eos.aurora.database.entities.RecipientUpdate
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
interface RecipientUpdateDao {
    @Insert
    fun insert(p: RecipientUpdate): Single<Long>

    @Query("SELECT * FROM RecipientUpdate WHERE uploaded = 0")
    fun getPendingUpload(): Maybe<List<RecipientUpdate>>

    @Query("SELECT COUNT(*) as result FROM RecipientUpdate WHERE uploaded = 0 LIMIT 1")
    fun getPendingUploadCount(): Maybe<Int>

    @Query("UPDATE RecipientUpdate SET uploaded = 1 WHERE updateId = :updateId")
    fun markUploaded(updateId: Int): Completable

    @Query("SELECT COUNT(*) as result FROM RecipientUpdate LIMIT 1")
    fun getCount(): Maybe<Int>
}
