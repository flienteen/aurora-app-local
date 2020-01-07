package com.persidius.eos.aurora.util

enum class RecipientStream(val displayName: String) {
    REZ("Rezidual"),
    BIO("Biodegradabil"),
    RPC("Hârtie și Carton"),
    RPM("Plastic și Metal"),
    RGL("Sticlă"),
    UNK("Necunoscut");

    companion object {
        fun fromDisplayName(displayName: String): RecipientStream {
            // walk the entire value hierarchy and figure out
            for(v in values()) {
                if(v.displayName == displayName) {
                    return v
                }
            }
            return UNK
        }
    }
}