package com.persidius.nemesis.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

class BTService(appContext: Context, module: BTModule): BroadcastReceiver() {
    private val BLUETOOTH_SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    enum class State (val i: Int) {
        DISABLED(0),
        ENABLED(1),
        CONNECTING(2),
        CONNECTED(3)
    }

    private val rnModule = module

    // locked w/ 'lock'
    private var activeDeviceId: String? = null

    // locked w/ 'lock'
    private var connectThread: Thread? = null

    private val lock = ReentrantLock()
    private val adapter: BluetoothAdapter? = (appContext.getSystemService((Context.BLUETOOTH_SERVICE)) as BluetoothManager?)?.adapter

    init {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        appContext.registerReceiver(this, filter)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                // we only care about *disabled* and *enabled* states
                when(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)) {
                    BluetoothAdapter.STATE_ON -> {
                        lock.withLock {
                            setState(State.ENABLED)
                            if (activeDeviceId != null) {
                                connect(activeDeviceId!!)
                            }
                        }
                    }

                    // If we're connected, stop/start
                    BluetoothAdapter.STATE_TURNING_OFF -> {
                        lock.withLock {
                            disconnect()
                            setState(State.DISABLED)
                        }
                    }
                }
            }
        }
    }

    fun isEnabled(): Boolean {
        return adapter?.isEnabled ?: false
    }

    fun enable(): Boolean {
        return adapter?.enable() ?: false
    }

    fun disable(): Boolean {
        return adapter?.disable() ?: false
    }

    data class ListDevice(val id: String, val name: String)
    fun deviceList(): List<ListDevice> {
        val devices = mutableListOf<ListDevice>()
        for(dev in adapter?.bondedDevices ?: listOf<BluetoothDevice>()) {
            devices.add(ListDevice(dev.address, dev.name))
        }
        return devices
    }

    fun getActiveDevice(): String? = lock.withLock {
        return activeDeviceId
    }

    private var device: BluetoothDevice? = null
    private var socket: BluetoothSocket? = null

    private var connectShutdown: AtomicBoolean = AtomicBoolean(false)

    // Used to suppress exceptions that would normally generate 'disconnect' or 'fail' events.
    private var suppressExceptions: AtomicBoolean = AtomicBoolean(false)

    private fun tryConnectForever() {
        while(!connectShutdown.get()) {
            try {
                Log.d("RNBT", "Connect loop...")
                socket = device?.createRfcommSocketToServiceRecord(BLUETOOTH_SPP_UUID)

                if (device == null || socket == null) {
                    Log.d("RNBT", "Connect loop null ref retry.")
                    continue
                }

                // maybe we're already connected?
                if (socket!!.isConnected) {
                    Log.d("RNBT", "Socket was connected!")
                    setState(State.CONNECTED)
                    break
                }

                // try connecting
                socket!!.connect()
                if(socket!!.isConnected) {
                    Log.d("RNBT", "Socket connected!")
                    setState(State.CONNECTED)
                    break
                }
            } catch (e: Exception) {
                Log.d("RNBT", "Connect failed w/ exception $e")
            }
        }
    }

    /**
     * NOTE: Must be called with 'lock'
     */
    private fun connect(id: String) {
        Log.d("RNBT", "Starting connect thread")
        suppressExceptions.set(false)
        connectShutdown.set(false)
        device = adapter?.getRemoteDevice(id)

        // don't do anything.
        if(device == null) {
            return
        }

        setState(State.CONNECTING)

        connectThread = thread(start = true) {
            tryConnectForever()
        }
    }

    /**
     * NOTE: This method *DOES NOT* trigger the 'onDisconnect' callback.
     * It is up to the user to migrate to an appropriate which will trigger it.
     * NOTE2: Must be called with 'lock'
     */
    private fun disconnect() {
        suppressExceptions.set(true)
        if(socket != null) {
            socket!!.close()
        }

        // We're still connecting.
        if(connectThread != null && connectThread!!.isAlive) {
            connectShutdown.set(true)
            connectThread!!.join()
        }
    }

    fun setActiveDevice(id: String?) {
        lock.withLock {
            if ((activeDeviceId == null && id == null) ||
                (activeDeviceId != null && id != null && activeDeviceId == id)) {
                return
            }

            // if currentId != null, then stop connecting service & read thread.
            if (activeDeviceId != null) {
                disconnect()

                if(id == null) {
                    setState(State.ENABLED)
                }
            }

            activeDeviceId = id
            if (id != null && state.get() != State.DISABLED) {
                connect(id)
            }
        }
    }



    // Decide initial state
    private var state: AtomicReference<State> = AtomicReference( if(isEnabled()) { State.ENABLED } else { State.DISABLED } )

    init {
        Log.d("RNBT", "Initial sate is " + state.get().name)
    }

    private fun setState(newState: State) {
        lock.withLock {
            val oldState = state.getAndSet(newState)
            if (oldState != newState) {
                Log.d("RNBT", "Enter new state ${newState.name} from oldState ${oldState.name}")
                if (newState == State.DISABLED) {
                    rnModule.emitDisabled()
                }

                if (newState == State.ENABLED && oldState == State.DISABLED) {
                    rnModule.emitEnabled()
                }

                // if we're leaving the 'CONNECTED' state
                // emit a 'disconnect' event
                if (oldState == State.CONNECTED) {
                    rnModule.emitDisconnected()
                }

                if (newState == State.CONNECTED) {
                    rnModule.emitConnected()
                }
            }
        }
    }

    private var readThread: Thread? = null

    /**
     * Must specify size of read. Otherwise reads entire available data.
     */
    fun read(size: Int?, promise: Promise) {
        lock.withLock {
            try {
                val inStream: InputStream = if (state.get() == State.CONNECTED && socket != null) {
                    socket!!.inputStream
                } else {
                    null
                } ?: throw IllegalStateException("Bluetooth is not in a state where it can read")

                if (readThread != null) {
                    throw IllegalAccessException("There is already a read call pending")
                }

                readThread = thread(start = true) {
                    try {
                        val readSize = size ?: inStream.available()
                        val data = ByteArray(readSize) { 0 }
                        val sizeRead = inStream.read(data)

                        val jsArr = Arguments.createArray()
                        data.slice(IntRange(start = 0, endInclusive = sizeRead - 1)).forEach {
                            jsArr.pushInt(it.toInt())
                        }

                        val jsMap = Arguments.createMap()
                        jsMap.putInt("size", sizeRead)
                        jsMap.putArray("data", jsArr)

                        promise.resolve(jsMap)
                    } catch (t: Throwable) {
                        if (!suppressExceptions.get()) {
                            disconnect()
                            Log.d("RNBT", "Disconnected")
                            lock.withLock {
                                Log.d("RNBT", "Attempting to launch 'connect' again with adid=$activeDeviceId")
                                if(activeDeviceId != null) {
                                    connect(activeDeviceId!!)
                                }
                            }
                        }
                        promise.reject("ReadError", t.toString())
                    } finally {
                        lock.withLock {
                            readThread = null
                        }
                    }
                }
            }
            catch(t: Throwable) {
                if(!suppressExceptions.get()) {
                    disconnect()
                }
                if (t is IllegalAccessError) {
                    promise.reject("IllegalAccessError", t.message)
                    return
                }
                if(t is IllegalStateException) {
                    promise.reject("IllegalStateError", t.message)
                    return
                }

                promise.reject("UnknownError", t.toString())
            }
        }
    }

    private var writeThread: Thread? = null
    fun write(data: ByteArray, promise: Promise) {
        lock.withLock {
            try {
                val outStream: OutputStream = if (state.get() == State.CONNECTED && socket != null) {
                    socket!!.outputStream
                } else {
                    null
                } ?: throw IllegalStateException("Bluetooth is not in a state where it can write")

                if (writeThread != null) {
                    throw IllegalAccessException("There is already a write call pending")
                }

                writeThread = thread(start = true) {
                    try {
                        Log.d("RNBT", "Writing ${data.size} bytes")
                        outStream.write(data)
                        promise.resolve(null)
                    } catch (t: Throwable) {
                        if (!suppressExceptions.get()) {
                            disconnect()
                            Log.d("RNBT", "Disconnected")
                            lock.withLock {
                                Log.d("RNBT", "Attempting to launch 'connect' again with adid=$activeDeviceId")
                                if(activeDeviceId != null) {
                                    connect(activeDeviceId!!)
                                }
                            }
                        }
                        promise.reject("WriteError", t.toString())
                    } finally {
                        lock.withLock {
                            writeThread = null
                        }
                    }
                }
            }
            catch(t: Throwable) {
                if(!suppressExceptions.get()) {
                    disconnect()
                }
                if (t is IllegalAccessError) {
                    promise.reject("IllegalAccessError", t.message)
                    return
                }
                if(t is IllegalStateException) {
                    promise.reject("IllegalStateError", t.message)
                    return
                }

                promise.reject("UnknownError", t.toString())
            }
        }
    }
}