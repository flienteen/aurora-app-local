package com.persidius.eos.aurora.authorization

enum class Role(val code: String) {
    LOGISTICS_VIEW_RECIPIENT("lvr"),
    LOGISTICS_VIEW_USER("lvu"),
    LOGISTICS_VIEW_GROUPS("lvg"),
    LOGISTICS_VIEW_TAGS("lvt"),

    LOGISTICS_EDIT_RECIPIENT("ler"),
    LOGISTICS_EDIT_USER("leu"),
    LOGISTICS_EDIT_GROP("leg"),

    LOGISTICS_ALLOC_RECIPIENT("lar"),
    LOGISTICS_ALLOC_GROUP("lag"),
    LOGISTICS_ALLOC_USER("lau"),

    // Allows Creation of tasks
    LOGISTICS_CREATE_TASK("lck"),

    // Allows viewing own tasks
    LOGISTICS_VIEW_TASK("lvk"),

    // Allows editing on tasks. Not used in mobile app.
    LOGISTICS_EDIT_TASK("lek"),

    // allows updating tasks.
    LOGISTICS_UPDATE_TASK("luk"),

    DISABLE_PAGINATION("dpp"),
    DEBUG("dbg");

    companion object {
        private val Codes = values().map { it.code to it }.toMap()
        fun fromCode(code: String): Role? {
            return Codes.get(code)
        }
    }
}
