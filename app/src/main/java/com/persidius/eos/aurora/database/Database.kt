package com.persidius.eos.aurora.database

import android.content.Context
import androidx.room.Room
import com.persidius.eos.aurora.database.dao.*

object Database {
    private lateinit var eos: EosDatabase

    fun init(applicationContext: Context) {
        // Add migrations here.
        eos = Room.databaseBuilder(applicationContext, EosDatabase::class.java, "n_eos.database")
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

    val group: GroupDao
        get() = eos.groupDao()

    val recLabel: RecommendedLabelDao
        get() = eos.recLabelDao()

    val recipientUpdates get() = eos.recipientUpdateDao()

    val recipientTags get() = eos.recipientTagDao()

    val recipientTagUpdates get() = eos.recipientTagUpdateDao()
}