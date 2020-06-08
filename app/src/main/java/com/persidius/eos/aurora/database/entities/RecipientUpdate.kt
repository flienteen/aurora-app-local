package com.persidius.eos.aurora.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/*
    val locId: Int? = null,
    val uatId: Int? = null,
    val size: String? = null,
    val stream: String? = null,
    val addressStreet: String? = null,
    val addressNumber: String? = null,
    val comments: String? = null,
    val groupId: String? = null,        // Empty String represents NULL in graphql
    val tag0: String? = null,           // Empty String represents NULL in graphql
    val tag1: String? = null           // Empty String represents NULL in graphql
    val labels: Map<String, String?>? = null
 */

@Entity(
    indices = [
        Index(value = ["eosId"], unique = false),
        Index(value = ["uploaded"], unique = false)
    ]
)
data class RecipientUpdate(
    @PrimaryKey(autoGenerate = true)
    val updateId: Int,

    val eosId: String,

    // Internal. For audit purposes. (time when this was created. ISO Timestring)
    val createdAt: String,
    // Internal. For audit purposes. (email of creator)
    val createdBy: String,
    // Internal. For audit purposes.
    val id: Int,

    val posLat: Double?,
    val posLng: Double?,

    val stream: String?,
    val size: String?,

    val addressStreet: String?,
    val addressNumber: String?,

    val uatId: Int?,
    val locId: Int?,

    val comments: String?,

    val labels: Map<String, String?>?,

    val groupId: String?,

    val uploaded: Boolean
)