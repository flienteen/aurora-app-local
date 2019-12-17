package com.persidius.eos.aurora.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.persidius.eos.aurora.database.dao.*
import com.persidius.eos.aurora.database.entities.*

@Database(
    entities = [County::class, Uat::class, Loc::class, Artery::class,
            Recipient::class, RecipientTag::class, Groups::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
abstract class EosDatabase: RoomDatabase() {
    abstract fun countyDao(): CountyDao

    abstract fun uatDao(): UatDao

    abstract fun locDao(): LocDao

    abstract fun arteryDao(): ArteryDao

    abstract fun recipientDao(): RecipientDao

    abstract fun recipientTagDao(): RecipientTagDao

    abstract fun groupsDao(): GroupsDao

    abstract fun recLabelDao(): RecommendedLabelDao
}