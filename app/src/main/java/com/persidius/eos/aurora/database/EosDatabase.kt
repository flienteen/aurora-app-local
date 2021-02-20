package com.persidius.eos.aurora.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.persidius.eos.aurora.database.dao.*
import com.persidius.eos.aurora.database.entities.*
import com.persidius.eos.aurora.database.fts.*

@Database(
    entities = [County::class, Uat::class, UatFTS::class, Loc::class,
        Artery::class, ArteryFTS::class, RecommendedLabel::class,
        Recipient::class, RecipientFTS::class, RecipientTag::class,
        RecipientUpdate::class, RecipientTagUpdate::class,
        Group::class, GroupFTS::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class EosDatabase : RoomDatabase() {
    abstract fun countyDao(): CountyDao
    abstract fun uatDao(): UatDao
    abstract fun locDao(): LocDao
    abstract fun arteryDao(): ArteryDao

    abstract fun recLabelDao(): RecommendedLabelDao

    abstract fun groupDao(): GroupDao

    abstract fun recipientDao(): RecipientDao
    abstract fun recipientUpdateDao(): RecipientUpdateDao

    abstract fun recipientTagDao(): RecipientTagDao
    abstract fun recipientTagUpdateDao(): RecipientTagUpdateDao
}