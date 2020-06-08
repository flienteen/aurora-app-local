package com.persidius.eos.aurora.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.persidius.eos.aurora.database.entities.RecipientTagUpdate
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
interface RecipientTagUpdateDao {
    @Insert
    fun insert(p: RecipientTagUpdate): Single<Long>

    @Query("SELECT * FROM RecipientTagUpdate WHERE uploaded = 0")
    fun getPendingUpload(): Maybe<List<RecipientTagUpdate>>

    @Query("UPDATE RecipientTagUpdate SET uploaded = 1 WHERE updateId = :updateId")
    fun markUploaded(updateId: Int): Completable

    @Query("SELECT COUNT(*) as result FROM RecipientTagUpdate WHERE uploaded = 0 LIMIT 1")
    fun getPendingUploadCount(): Maybe<Int>

    @Query("SELECT COUNT(*) as result FROM RecipientTagUpdate LIMIT 1")
    fun getCount(): Maybe<Int>
}
