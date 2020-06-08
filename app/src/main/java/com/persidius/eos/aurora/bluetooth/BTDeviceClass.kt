package com.persidius.eos.aurora.bluetooth

import android.bluetooth.BluetoothAdapter
import com.persidius.nemesis.bluetooth.BTService
import io.reactivex.Observable

/**
 * The BT Device should store the socket? -> no.
 * It should act solely as an event listener for onRead and have a 'write' method
 * that it can use.
 *
 */
interface BTDeviceAdapter {
    /**
     * Friendly DisplayName of this device class
     */
    fun name(): String

    /**
     * Initialise the device class
     */
    fun start(read: Observable<ByteArray>)
}