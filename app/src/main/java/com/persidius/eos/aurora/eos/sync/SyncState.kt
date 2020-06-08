package com.persidius.eos.aurora.eos.sync

enum class State {
    NOT_READY,
    DEFINITIONS,
    RECIPIENTS,
    GROUPS,

    UPDATE_RECIPIENTS,

    ABORTED;
}