package com.persidius.eos.aurora.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.persidius.eos.aurora.database.entities.Session
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

@Dao
abstract class SessionDao {
    @Insert
    abstract fun _insertSession(s: Session): Single<Long>


    @Query("SELECT * FROM Session WHERE id = :id")
    abstract fun getById(id: Int): Maybe<Session>

    fun createSession(): Maybe<Session> {
        val s = Session(-0,
            createdAt = System.currentTimeMillis() / 1000,
            refCount = 1,
            open = true,
            uploaded = false,
            txId = null)

        return _insertSession(s).subscribeOn(Schedulers.io())
            .flatMapMaybe { id -> getById(id.toInt()).subscribeOn(Schedulers.io()) }
    }

    @Query("UPDATE Session SET refCount = refCount + :refCount, open = CASE WHEN (refCount + :refCount) > 0 THEN 1 ELSE 0 END WHERE id = :id")
    abstract fun updateRefCount(id: Long, refCount: Int): Completable

    @Query("SELECT * FROM Session WHERE open = 0 AND uploaded = 0")
    abstract fun getPendingUpload(): Maybe<List<Session>>

    @Query("UPDATE SESSION SET txId = :txId, uploaded = :uploaded WHERE id = :id")
    abstract fun setUploaded(id: Long, uploaded: Boolean, txId: String?): Completable

    @Query("DELETE FROM SESSION WHERE id = :id")
    abstract fun deleteSession(id: Long): Completable
}