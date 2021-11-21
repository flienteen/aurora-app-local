package com.persidius.eos.aurora.util

import com.persidius.eos.aurora.type.RecipientLifecycle

object RecipientLifecycleDisplayName {
  fun get(it: RecipientLifecycle?): String {
    val translation = when (it) {
      RecipientLifecycle.LABEL_ONLY -> "Eticheta"
      RecipientLifecycle.TAG_ATTACHED -> "Tag instalat"
      RecipientLifecycle.ACTIVE -> "Activ"
      RecipientLifecycle.DELETED -> "Sters"
      else -> null
    }
    return translation ?: it.toString()
  }

  fun get(it: String): String {
    val enum = try {
      RecipientLifecycle.valueOf(it)
    } catch (e: Exception) {
      null
    }
    return get(enum)
  }

  fun toValue(it: String?): RecipientLifecycle {
    return when (it) {
      "Eticheta" -> RecipientLifecycle.LABEL_ONLY
      "Tag instalat" -> RecipientLifecycle.TAG_ATTACHED
      "Activ" -> RecipientLifecycle.ACTIVE
      "Sters" -> RecipientLifecycle.DELETED
      else -> RecipientLifecycle.UNKNOWN__
    }
  }
}