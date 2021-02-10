package com.persidius.eos.aurora.bluetooth.devices

enum class LFReaderType {
    TypeA,      // Black ones, older model (msg format is FF 00 11 22 33 44)
    TypeB       // Silver ones, newer model (msg format is AA FF 00 11 22 33 44 CC)
}