package com.persidius.eos.aurora.eos.sync

enum class SyncState {
    WAIT_VALID_SESSION,
    DEFINITIONS,
    RECIPIENTS,
    TAGS,
    GROUPS,

    UPDATE_RECIPIENTS,
    UPDATE_TAGS,
    UPLOAD_COLLECTIONS,

    SYNC_RECIPIENTS,
    SYNC_TAGS,
    SYNC_GROUPS,


    // Timeout.
    SYNC_WAIT
}