package com.persidius.eos.aurora.rfidService

import android.bluetooth.BluetoothAdapter

interface RFIDDevice {
    /**
     * Initialise the device class
     */
    fun init(service: RFIDService)

    /**
     * Connect to the device
     */
    fun connect(address: String, adapter: BluetoothAdapter)

    /**
     * Read next data from device
     */
    fun read(): ByteArray?

    /**
     * Should terminate all access to the device
     */
    fun disconnect()
}