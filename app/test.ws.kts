enum class Role(val code: String) {
    FIRST("1"),
    SECOND("2");

    companion object {
        val fromName = values().map { it.code to it }.toMap()
    }
}

Role.fromName["1"]

