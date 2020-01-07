package com.persidius.eos.aurora.util

enum class RecipientSize(val displayName: String, val visible: Boolean) {
    SIZE_1100L("1.100L", true),
    SIZE_240L("240L", true),
    SIZE_120L("120L", true);

    companion object {
        fun getVisibleSizes(): List<String> =
            values()
                .filter { v -> v.visible }
                .map { v -> v.displayName }

        fun fromDisplayName(displayName: String): RecipientSize =
            values()
                .find { v -> v.displayName == displayName}!!
    }
}