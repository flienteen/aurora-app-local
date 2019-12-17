package com.persidius.eos.aurora.database.dao

import androidx.room.*
import com.persidius.eos.aurora.database.entities.RecipientTag
import io.reactivex.Completable
import io.reactivex.Maybe

@Dao
interface RecipientTagDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tags: List<RecipientTag>): Completable

    @Query("DELETE FROM recipientTag")
    fun deleteAll(): Completable

    @Query("SELECT * FROM recipientTag WHERE recipientId = :recipientId")
    fun getByRecipientId(recipientId: String): Maybe<List<RecipientTag>>

    @Query("SELECT * FROM recipientTag WHERE tag = :tag")
    fun getByTag(tag: String): Maybe<RecipientTag>
}
