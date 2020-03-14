package com.persidius.eos.aurora.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.persidius.eos.aurora.database.dao.*

object Database {
    private lateinit var eos: EosDatabase

    private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(_db: SupportSQLiteDatabase) {
            _db.execSQL("CREATE TABLE IF NOT EXISTS `Task` (`id` INTEGER NOT NULL, `gid` TEXT, `assignedTo` TEXT, `validFrom` INTEGER NOT NULL, `validTo` INTEGER NOT NULL, `updatedBy` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `status` TEXT NOT NULL, `posLat` REAL, `posLng` REAL, `comments` TEXT NOT NULL, `uatId` INTEGER NOT NULL, `locId` INTEGER NOT NULL, `countyId` INTEGER NOT NULL, `groups` TEXT NOT NULL, `users` TEXT NOT NULL, `recipients` TEXT NOT NULL, PRIMARY KEY(`id`))")
            _db.execSQL("CREATE INDEX IF NOT EXISTS `index_Task_countyId` ON `Task` (`countyId`)")
            _db.execSQL("CREATE INDEX IF NOT EXISTS `index_Task_uatId` ON `Task` (`uatId`)")
            _db.execSQL("CREATE INDEX IF NOT EXISTS `index_Task_locId` ON `Task` (`locId`)")
            _db.execSQL("CREATE INDEX IF NOT EXISTS `index_Task_updatedAt` ON `Task` (`updatedAt`)")
            _db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `TaskFTS` USING FTS4(`id` INTEGER NOT NULL, `status` TEXT NOT NULL, `groups` TEXT NOT NULL, `users` TEXT NOT NULL, `recipients` TEXT NOT NULL, tokenize=unicode61, content=`Task`)")
            _db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_TaskFTS_BEFORE_UPDATE BEFORE UPDATE ON `Task` BEGIN DELETE FROM `TaskFTS` WHERE `docid`=OLD.`rowid`; END")
            _db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_TaskFTS_BEFORE_DELETE BEFORE DELETE ON `Task` BEGIN DELETE FROM `TaskFTS` WHERE `docid`=OLD.`rowid`; END")
            _db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_TaskFTS_AFTER_UPDATE AFTER UPDATE ON `Task` BEGIN INSERT INTO `TaskFTS`(`docid`, `id`, `status`, `groups`, `users`, `recipients`) VALUES (NEW.`rowid`, NEW.`id`, NEW.`status`, NEW.`groups`, NEW.`users`, NEW.`recipients`); END")
            _db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_TaskFTS_AFTER_INSERT AFTER INSERT ON `Task` BEGIN INSERT INTO `TaskFTS`(`docid`, `id`, `status`, `groups`, `users`, `recipients`) VALUES (NEW.`rowid`, NEW.`id`, NEW.`status`, NEW.`groups`, NEW.`users`, NEW.`recipients`); END")
            _db.execSQL("CREATE TABLE IF NOT EXISTS `TaskPatch` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `gid` TEXT, `taskId` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `sessionId` INTEGER, `comments` TEXT, `recipients` TEXT, `uatId` INTEGER, `locId` INTEGER, `posLat` REAL, `posLng` REAL)")
            _db.execSQL("CREATE INDEX IF NOT EXISTS `index_TaskPatch_sessionId` ON `TaskPatch` (`sessionId`)")
            _db.execSQL("CREATE INDEX IF NOT EXISTS `index_TaskPatch_taskId` ON `TaskPatch` (`taskId`)")
        }
    }

    fun init(applicationContext: Context) {
        // Add migrations here.
        eos = Room.databaseBuilder(applicationContext, EosDatabase::class.java, "eos.database")
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    val county: CountyDao
        get() = eos.countyDao()

    val uat: UatDao
        get() = eos.uatDao()

    val loc: LocDao
        get() = eos.locDao()

    val artery: ArteryDao
        get() = eos.arteryDao()

    val recipient: RecipientDao
        get() = eos.recipientDao()

    val task: TaskDao
        get() = eos.taskDao()

    val recipientTag: RecipientTagDao
        get() = eos.recipientTagDao()

    val groups: GroupsDao
        get() = eos.groupsDao()

    val user: UserDao
        get() = eos.userDao()

    val recLabel: RecommendedLabelDao
        get() = eos.recLabelDao()


    val session get() = eos.sessionDao()

    val recipientPatch get() = eos.recipientPatchDao()

    val taskPatch get() = eos.taskPatchDao()

}