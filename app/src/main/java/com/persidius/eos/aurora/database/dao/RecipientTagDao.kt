package com.persidius.eos.aurora.database.dao

import androidx.room.*
import com.persidius.eos.aurora.database.LongQueryResult
import com.persidius.eos.aurora.database.entities.RecipientTag
import io.reactivex.Completable
import io.reactivex.Maybe

@Dao
interface RecipientTagDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tags: List<RecipientTag>): Completable

    @Query("DELETE FROM RecipientTag")
    fun deleteAll(): Completable

    @Query("SELECT * FROM RecipientTag WHERE recipientId = :recipientId")
    fun getByRecipientId(recipientId: String): Maybe<List<RecipientTag>>

    @Query("SELECT * FROM RecipientTag WHERE tag = :tag")
    fun getByTag(tag: String): Maybe<RecipientTag>

    @Query("SELECT COUNT(tag) AS result FROM RecipientTag")
    fun getCount(): Maybe<LongQueryResult>
}
