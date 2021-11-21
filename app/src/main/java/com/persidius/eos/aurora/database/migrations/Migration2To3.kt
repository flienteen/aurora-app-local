package com.persidius.eos.aurora.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration2To3: Migration(2, 3) {
  override fun migrate(database: SupportSQLiteDatabase) {
    database.execSQL("ALTER TABLE Recipient ADD lifecycle TEXT NOT NULL DEFAULT ('ACTIVE')")
    database.execSQL("ALTER TABLE RecipientUpdate ADD lifecycle TEXT")
  }
}