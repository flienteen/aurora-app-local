package com.persidius.eos.aurora.util

data class Optional<T>(val value: T?) {
    fun isPresent(): Boolean {
        return value != null
    }

    fun get(): T {
        return value!!
    }
}

fun <T> T?.asOptional() = Optional(this)