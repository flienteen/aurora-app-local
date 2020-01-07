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
            "eos.database")
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

}