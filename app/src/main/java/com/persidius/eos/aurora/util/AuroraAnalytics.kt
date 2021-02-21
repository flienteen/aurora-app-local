package com.persidius.eos.aurora.util

object AuroraAnalytics {
    object Event {
        const val REASSIGN_WARNING = "reassign_warning"
        const val REASSIGN_WARNING_IGNORED = "reassign_warning_ignored"
        const val REASSIGN_WARNING_ACKNOWLEDGED = "reassign_warning_acknowledged"
    }

    object Params {
        const val RECIPIENT_EOS_ID = "recipient_id"
        const val RFID_TAG = "rfid_tag"

        // Whether or not the REASSIGN_WARNING setting was enabled or not
        const val REASSIGN_WARNING_ENABLED = "reassign_warning_enabled"
    }

}