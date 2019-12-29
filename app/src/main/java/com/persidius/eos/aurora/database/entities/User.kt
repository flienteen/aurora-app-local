package com.persidius.eos.aurora.database.entities

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/*
        active
        id
        name
        idNumber
        type
        billingType
        billingData {
            ... on UserBillingData_PerPerson {
                persons
            }

            ... on UserBillingData_PerPerson_RA {
                persons
            }
        }
        addressNumber
        addressStreet
        labels
        updatedAt
        uat {
            id
        }
        county {
            id
        }
        loc {
            id
        }
        comments
        groupId
    }
 */

@Entity(
    indices = [
        Index(value = ["countyId"], unique = false),
        Index(value = ["uatId"], unique = false),
        Index(value = ["locId"], unique = false),
        Index(value = ["updatedAt"], unique = false)
    ]
)
data class User (
    @PrimaryKey
    @NonNull
    val id: String,

    val name: String,
    val idNumber: String,

    val labels: List<String>,
    val comments: String,
    val active: Boolean,
    val updatedAt: Long,
    val groupId: String?,

    val type: String,

    val billingType: String,
    val persons: Int?,

    val countyId: Int,
    val uatId: Int,
    val locId: Int
)
