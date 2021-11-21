package com.persidius.eos.aurora.util

import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object DateUtil {

  private val ISO_8601_FORMAT = DateTimeFormatter.ISO_INSTANT
  fun unixToISOTimestamp(epochSeconds: Long): String {
    return ISO_8601_FORMAT.format(Instant.ofEpochSecond(epochSeconds))
  }

  fun nowISOTimestamp(): String = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
}