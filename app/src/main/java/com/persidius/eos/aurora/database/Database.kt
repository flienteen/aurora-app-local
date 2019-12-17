package com.persidius.eos.aurora.database

import android.content.Context
import androidx.room.Room
import com.persidius.eos.aurora.database.dao.*

object Database {
    private lateinit var eos: EosDatabase

    fun init(applicationContext: Context) {
        // Add migrations here.
        eos = Room.databaseBuilder(
            applicationContext,
            EosDatabase::class.java,
            "eos-database").build()
    }

    val county: CountyDao
        get() { return eos.countyDao() }

    val uat: UatDao
        get() { return eos.uatDao() }

    val loc: LocDao
        get() { return eos.locDao() }

    val artery: ArteryDao
        get() { return eos.arteryDao() }

    val recipient: RecipientDao
        get() { return eos.recipientDao() }

    val recipientTag: RecipientTagDao
        get() { return eos.recipientTagDao() }

    val groups: GroupsDao
        get() { return eos.groupsDao() }

    val recLabel: RecommendedLabelDao
        get() { return eos.recLabelDao() }
}