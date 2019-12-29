package com.persidius.eos.aurora.util

import android.graphics.Color
import com.persidius.eos.aurora.R

/*
    <color name="colorREZ">#212121</color>
    <color name="colorRGL">#00c853</color>
    <color name="colorRPC">#2962ff</color>
    <color name="colorRPM">#ffab00</color>
    <color name="colorBIO">#bf360c</color>
 */

enum class StreamColors(val color: Color) {
    REZ(Color.valueOf(0x212121)),
    RGL(Color.valueOf(0x00C853)),
    RPC(Color.valueOf(0x2926FF)),
    RPM(Color.valueOf(0xFFAB00)),
    BIO(Color.valueOf(0xBF360C)),
    UNK(Color.valueOf(0xff1744));

    companion object {
        private val nameMap = values().associateBy { v -> v.name }
        fun from(s: String) = nameMap[s] ?: UNK
    }
}