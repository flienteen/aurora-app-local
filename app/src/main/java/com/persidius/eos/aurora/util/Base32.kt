package com.persidius.eos.aurora.util

import kotlin.math.pow

// Convert Base32 to int
const val ab = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
val abIndex = ab.associateWith { c -> ab.indexOf(c).toLong() }


object Base32 {
    fun toLong(s: String) = s.map { c -> abIndex.getOrDefault(c, 0) }
            .asReversed()
            .reduceIndexed { ix: Int, p: Long, c: Long -> p + c * (32.0.pow(ix)).toInt() }
}
