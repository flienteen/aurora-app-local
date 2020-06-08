package com.persidius.eos.aurora.bluetooth.devices

import com.persidius.eos.aurora.bluetooth.BTDeviceClass
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

// TODO: Implement me!
class LFReader: BTDeviceClass {
    companion object {
        const val name = "LF Reader"
    }
    override fun start(read: Observable<ByteArray>, write: (data: ByteArray) -> Completable, tags: PublishSubject<String>) {
    }
}

//package com.persidius.eos.aurora.bluetooth
//
//import android.bluetooth.BluetoothAdapter
//import android.bluetooth.BluetoothDevice
//import android.bluetooth.BluetoothSocket
//import java.util.*
//import java.util.concurrent.locks.ReentrantLock
//
//class LFReader {
//    companion object {
//        const val DEVICE_TYPE_NAME = "LF Reader (argintiu/negru)"
//        private val BLUETOOTH_SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
//    }
//
//
//    private var currentDevice: BluetoothDevice? = null
//    private var deviceSocket: BluetoothSocket? = null
//    private val lock = ReentrantLock()
//    /**
//     * Gets called when device was *not* connected and not reported as
//     * disconnected (onDisconnect called)
//     */
//    override fun connect(address: String, adapter: BluetoothAdapter) {
//        currentDevice = adapter.getRemoteDevice(address)
//        try {
//            val devSocket = currentDevice?.createRfcommSocketToServiceRecord(BLUETOOTH_SPP_UUID)
//
//            lock.lock()
//            deviceSocket = devSocket
//            if(deviceSocket == null) {
//                service.onConnectError()
//                lock.unlock()
//                return
//            }
//            lock.unlock()
//
//            deviceSocket!!.connect()
//            lock.lock()
//            service.onConnected()
//            lock.unlock()
//        } catch(e: Exception) {
//            // TODO: check if this is necessary
//            // deviceSocket?.close()
//            service.onConnectError()
//        }
//    }
//
//    override fun read(): ByteArray? {
//        lock.lock()
//        val socket = deviceSocket
//        lock.unlock()
//
//        socket ?: return null
//
//        val input = socket.inputStream
//
//        // Two types of messages from (2 types) of scanners:
//        // Type 1:
//        // AA FF 00 11 22 33 44 CC
//        // Type 2:
//        // FF 00 11 22 33 44
//
//        // Type 1 message
//        try {
//            return when(input.read()) {
//                0xAA -> {
//                    val data = ByteArray(6)
//                    val read = input.read(data, 0, 6)
//                    return if(read == 6) data.sliceArray(1..5) else null
//                }
//                0xFF -> {
//                    val data = ByteArray(5)
//                    val read = input.read(data, 0, 5)
//                    return if(read == 5) data else null
//                }
//                else -> null
//            }
//        } catch(e: Exception) {
//            // NOTE: This should indicate a disconnect.
//            socket.close()
//            service.onDisconnect()
//            return null
//        }
//    }
//
//    override fun disconnect() {
//        lock.lock()
//        deviceSocket?.close()
//        deviceSocket = null
//        lock.unlock()
//        service.onDisconnect()
//    }
//}
