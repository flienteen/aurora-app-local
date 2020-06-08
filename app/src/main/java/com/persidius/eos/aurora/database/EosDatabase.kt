package com.persidius.eos.aurora.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.persidius.eos.aurora.database.dao.*
import com.persidius.eos.aurora.database.entities.*
import com.persidius.eos.aurora.database.fts.*

@Database(
    entities = [County::class, Uat::class, UatFTS::class, Loc::class,
        Artery::class, ArteryFTS::class, RecommendedLabel::class, Session::class,
        Recipient::class, RecipientTag::class, RecipientFTS::class, RecipientPatch::class,
        Task::class, TaskFTS::class, TaskPatch::class,
        Groups::class, GroupsFTS::class, User::class
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

    abstract fun recipientDao(): RecipientDao

    abstract fun taskDao(): TaskDao

    abstract fun recipientTagDao(): RecipientTagDao

    abstract fun groupsDao(): GroupsDao

    abstract fun recLabelDao(): RecommendedLabelDao

    abstract fun userDao(): UserDao

    abstract fun recipientPatchDao(): RecipientPatchDao

    abstract fun taskPatchDao(): TaskPatchDao

    abstract fun sessionDao(): SessionDao
}