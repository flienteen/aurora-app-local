package com.persidius.eos.aurora.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.persidius.eos.aurora.database.LongQueryResult
import com.persidius.eos.aurora.database.entities.Recipient
import io.reactivex.Completable
import io.reactivex.Maybe

@Dao
interface RecipientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(recipients: List<Recipient>): Completable

    @Query("DELETE FROM Recipient")
    fun deleteAll(): Completable

    @Query("SELECT * FROM Recipient WHERE id = :recipientId")
    fun getById(recipientId: String): Maybe<Recipient>

    @Query("SELECT MAX(updatedAt) AS result FROM Recipient LIMIT 1")
    fun lastUpdatedAt(): Maybe<LongQueryResult>

    @Query("SELECT COUNT(id) AS result FROM Recipient")
    fun getCount(): Maybe<LongQueryResult>

    @Query("SELECT * FROM Recipient JOIN RecipientFTS ON (RecipientFTS.rowid = Recipient.rowid) WHERE RecipientFTS MATCH :term")
    fun search(term: String): Maybe<List<Recipient>>
}
