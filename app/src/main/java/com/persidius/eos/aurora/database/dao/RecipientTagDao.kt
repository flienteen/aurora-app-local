package com.persidius.eos.aurora.database.dao

import com.persidius.eos.aurora.database.entities.RecipientTag
import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
interface RecipientTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tags: List<RecipientTag>): Completable

    @Query("DELETE FROM RecipientTag")
    fun deleteAll(): Completable

    @Query("SELECT * FROM RecipientTag WHERE tag = :tag")
    fun getByTag(tag: String): Maybe<RecipientTag>

    @Query("SELECT * FROM RecipientTag WHERE recipientId = :recipientId")
    fun getByRecipientId(recipientId: String): Single<List<RecipientTag>>

    @Query("SELECT MAX(id) AS result FROM RecipientTag LIMIT 1")
    fun maxId(): Maybe<Int>

    @Query("SELECT COUNT(id) AS result FROM RecipientTag")
    fun getCount(): Maybe<Int>
}
