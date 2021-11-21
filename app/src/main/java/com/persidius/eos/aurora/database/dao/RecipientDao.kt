package com.persidius.eos.aurora.database.dao

import androidx.room.*
import com.persidius.eos.aurora.database.entities.Recipient
import io.reactivex.Completable
import io.reactivex.Maybe

@Dao
interface RecipientDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun rxInsert(recipients: List<Recipient>): Completable

  @Query("DELETE FROM Recipient")
  fun rxDeleteAll(): Completable

  @Query("SELECT * FROM Recipient WHERE eosId = :eosId")
  fun getById(eosId: String): Maybe<Recipient>

  @Query("SELECT MAX(id) AS result FROM Recipient LIMIT 1")
  fun maxId(): Maybe<Int>

  @Query("SELECT COUNT(id) AS result FROM Recipient")
  fun getCount(): Maybe<Int>

  @Query("SELECT * FROM Recipient JOIN RecipientFTS ON (RecipientFTS.rowid = Recipient.rowid) WHERE RecipientFTS MATCH :term")
  fun search(term: String): Maybe<List<Recipient>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(recipients: List<Recipient>)

  @Query("DELETE FROM Recipient")
  suspend fun deleteAll()

}
