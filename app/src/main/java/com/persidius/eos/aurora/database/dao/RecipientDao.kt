package com.persidius.eos.aurora.database.dao

import androidx.room.*
import com.persidius.eos.aurora.database.entities.Recipient
import io.reactivex.Completable
import io.reactivex.Maybe

@Dao
interface RecipientDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(recipients: List<Recipient>): Completable

    @Query("DELETE FROM recipient")
    fun deleteAll(): Completable

    @Query("SELECT * FROM recipient WHERE id = :recipientId")
    fun getById(recipientId: String): Maybe<Recipient>
}
