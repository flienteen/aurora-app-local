package com.persidius.eos.aurora.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


object Migration3To4: Migration(3, 4) {
  override fun migrate(database: SupportSQLiteDatabase) {
    database.execSQL("CREATE TABLE IF NOT EXISTS `Vehicle` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleLicensePlate` TEXT NOT NULL, `countyId` INTEGER NOT NULL)")
    database.execSQL("CREATE TABLE IF NOT EXISTS `Collection` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `extId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `posLat` REAL NOT NULL, `posLng` REAL NOT NULL, `vehicleLicensePlate` TEXT NOT NULL, `tag` TEXT NOT NULL, `uploaded` INTEGER NOT NULL, `countyId` INTEGER)")
    database.execSQL("CREATE INDEX IF NOT EXISTS `index_Collection_tag` ON `Collection` (`tag`)")
    database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_Collection_createdAt_tag` ON `Collection` (`createdAt`, `tag`)")
  }
}