package com.persidius.eos.aurora.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.persidius.eos.aurora.database.entities.Collection
import io.reactivex.Completable
import io.reactivex.Maybe

@Dao
interface CollectionDao {
  @Insert
  fun insert(p: Collection): Completable

  @Query("SELECT * FROM collection WHERE tag = :tagId AND createdAt >= :createdAt")
  fun findRecentForTag(tagId: String, createdAt: Long): Maybe<List<Collection>>

  @Query("SELECT * FROM Collection WHERE uploaded = 0")
  fun getPendingUpload(): Maybe<List<Collection>>

  @Query("UPDATE Collection SET uploaded = 1 WHERE id = :id")
  fun markUploaded(id: Int): Completable
}