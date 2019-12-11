package com.persidius.eos.aurora.authorization

object Role {
    val LOGISTICS_VIEW_RECIPIENT = "lvr"
    val LOGISTICS_VIEW_USER = "lvu"
    val LOGISTICS_VIEW_GROUPS = "lvg"
    val LOGISTICS_VIEW_TAGS = "lvt"

    val LOGISTICS_EDIT_RECIPIENT = "ler"
    val LOGISTICS_EDIT_USER = "leu"
    val LOGISTICS_EDIT_GROUP = "leg"

    val LOGISTICS_ALLOC_RECIPIENT = "lar"
    val LOGISTICS_ALLOG_GROUP = "lag"
    val LOGISTICS_ALLOC_USER = "lau"

    // Allows creation of tasks.
    val LOGISTICS_CREATE_TASK = "lck"

    // Allows viewing tasks
    val LOGISTICS_VIEW_TASK = "lvk"

    // Allows editing (aka assigning/removing/setting every other attribute) on tasks
    // not used in mobile app
    val LOGISTICS_EDIT_TASK = "lek"

    // Allows updating tasks (updating existing or newly created tasks)
    val LOGISTICS_UPDATE_TASK = "luk"

    val DISABLE_PAGINATION = "dpp"
    val DEBUG = "dbg"
}
