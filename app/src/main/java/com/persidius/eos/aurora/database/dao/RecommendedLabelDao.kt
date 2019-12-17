package com.persidius.eos.aurora.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.persidius.eos.aurora.database.entities.RecommendedLabel
import io.reactivex.Completable
import io.reactivex.Maybe

@Dao
interface RecommendedLabelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(labels: List<RecommendedLabel>): Completable

    @Query("DELETE FROM recommendedLabel")
    fun deleteAll(): Completable

    @Query("SELECT * FROM recommendedLabel where label = :label")
    fun getForLabel(label: String): Maybe<RecommendedLabel>

    @Query("SELECT * FROM recommendedLabel")
    fun getAll(): Maybe<List<RecommendedLabel>>
}
