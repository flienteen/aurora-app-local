package com.persidius.eos.aurora.auth

enum class Role {
    VIEW_RECIPIENT,
    VIEW_GROUP,
    VIEW_TAGS,

    UPDATE_RECIPIENT,
    UPDATE_TAGS,

    DISABLE_PAGINATION,

    CREATE_COLLECTION,
    DEBUG;
}
