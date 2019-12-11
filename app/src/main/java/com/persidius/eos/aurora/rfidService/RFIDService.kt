package com.persidius.eos.aurora.rfidService

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class RFIDService(private val applicationContext: Context): BroadcastReceiver() {

    enum class State (val v: Int){
        DISABLED(0),
        BT_DISABLED(1),
        NOT_CONFIGURED(2),
        CONNECTING(3),
        CONNECTED(4)
    }

    val liveState = MutableLiveData<State>(State.DISABLED)
    val liveData = MutableLiveData<String?>(null)

    // Mutating these, you need to hold the lock.

    /**
     * Whether to keep running the main loop, or shut it down.
     */
    private val shutdown = AtomicBoolean(false)

    /**
     * Whether the service is connected to the device
     */
    private val connected = AtomicBoolean(false)

    /**
     * Device Address
     */
    private val deviceAddress = AtomicReference<String>("")

    /**
     * Device class
     */
    private val device = AtomicReference<RFIDDevice?>(null)

    /**
     * The service thread. To be used when waiting for the service thread
     * to exit.
     */
    private val serviceThread = AtomicReference<Thread?>(null)

    /**
     * Service State
     */
    private val state: AtomicReference<State> = AtomicReference(State.DISABLED)

    /**
     * Service Thread Semaphore
     */
    private val serviceThreadSignal: Semaphore = Semaphore(0)

    // This is read-only after onCreate.
    // we might not have access to btAdapter, but we try anyway.
    private val btAdapter: BluetoothAdapter = (applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private val listener = SharedPreferenceChangeListener()

    init {
        Log.d("RFID", "init")

        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        prefs.registerOnSharedPreferenceChangeListener(listener)

        if (prefs.getBoolean("rfidEnable", false)) {
            start()
        }

        // register as the intent receiver
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        applicationContext.registerReceiver(this, filter)
    }

    override fun onReceive(context: Context?, intent: Intent?) {

        when(intent?.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                // we only care about *disabled* and *enabled* states
                when(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)) {
                    BluetoothAdapter.STATE_ON -> {
                        if(this.state.get() === State.BT_DISABLED) {
                            stop(true)
                            start()
                        }
                    }

                    // If we're connected, stop/start
                    BluetoothAdapter.STATE_TURNING_OFF -> {
                        val serviceState = this.state.get()
                        if(serviceState == State.CONNECTED || serviceState == State.CONNECTING) {
                            stop(true)
                            start()
                        }
                    }
                }
            }
        }
    }

    fun destroy() {
        // Stop main thread & dispose
        stop(true)

        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    inner class SharedPreferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences,
            key: String?
        ) {
            when (key) {
                "rfidEnable" -> {
                    // Start/stop based on rfidEnable
                    val enabled = sharedPreferences.getBoolean(key, false)
                    if (enabled) {
                        start(true)
                    } else {
                        stop(true)
                    }
                }
                "rfidDeviceType" -> {
                    val enabled = sharedPreferences.getBoolean("rfidEnable", false)
                    val deviceType = sharedPreferences.getString(key, "")

                    if (enabled) {
                        stop(true)
                        if (!setDeviceType(deviceType!!)) {
                            setState(State.NOT_CONFIGURED)
                        } else {
                            start()
                        }
                    }
                }
                "rfidDeviceAddress" -> {
                    val enabled = sharedPreferences.getBoolean("rfidEnable", false)
                    val deviceAddress = sharedPreferences.getString(key, "")
                    if (enabled) {
                        stop(true)
                        if (!setDeviceAddress(deviceAddress!!)) {
                            setState(State.NOT_CONFIGURED)
                        } else {
                            start()
                        }
                    }
                }
            }
        }
    }

    /**
     * Starts a window that asks for access to the bluetooth device
     */
    private fun requestEnableBluetooth() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        applicationContext.startActivity(intent)
    }

    /**
     * Return a list of devices to use
     */
    fun getDeviceList(): List<String> {
        val deviceNames = ArrayList<String>()
        for(dev in btAdapter.bondedDevices) {
            deviceNames.add("%s\n%s".format(dev.name, dev.address))
        }
        return deviceNames
    }

    fun getDeviceTypeList(): List<String> {
        return listOf(
            LFReader.DEVICE_TYPE_NAME
        )
    }

    private fun setDeviceAddress(newDeviceAddress: String): Boolean {
        if(newDeviceAddress !in getDeviceList()) {
            return false
        }

        deviceAddress.set(newDeviceAddress.split("\n")[1])
        return true
    }

    private fun setDeviceType(deviceType: String): Boolean {
        if(deviceType !in getDeviceTypeList()) {
            return false
        }

        val rfidDevice: RFIDDevice? = when(deviceType) {
            LFReader.DEVICE_TYPE_NAME -> {
                val dev = LFReader()
                dev.init(this)
                dev
            }
            else -> return false
        }

        device.set(rfidDevice)
        return true
    }

    private fun start(userInitiated: Boolean = false) {
        // we have a serviceThread running & there's no shutdown going on
        // no reason to re-run the start procedure.
        if(serviceThread.get() !== null && !shutdown.get()) {
            return
        }

        // Refuse to start if btAdapter is not enabled.
        if(!btAdapter.isEnabled) {
            setState(State.BT_DISABLED)

            if(userInitiated) {
                requestEnableBluetooth()
            }

            return
        }

        // Check if we can config the current bt device/current device type
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        Log.d("RFID", "Starting service...")
        val deviceAddress = prefs.getString("rfidDeviceAddress", "")
        val deviceType = prefs.getString("rfidDeviceType", "")

        // start the connect thread.
        if(!setDeviceAddress(deviceAddress!!) || !setDeviceType(deviceType!!)) {
            setState(State.NOT_CONFIGURED)
            return
        }

        shutdown.set(false)
        connected.set(false)

        serviceThread.set(thread(
            start = true
        ) {
            // Main 'connect' loop.
            while(!shutdown.get()) {

                // On adapter disabled: return.
                if(!btAdapter.isEnabled) {
                    setState(State.BT_DISABLED)
                    break
                }
                setState(State.CONNECTING)

                // attempt to run the .connect method
                val localDeviceAddress = this.deviceAddress.get()
                val localDevice = this.device.get()

                // don't lock the state while waiting for a @connect
                val connectThread = thread(start = true) {
                    localDevice?.connect(localDeviceAddress, btAdapter)
                }

                while(connectThread.isAlive) {
                    serviceThreadSignal.tryAcquire( 50, TimeUnit.MILLISECONDS)
                    // if shutdown, kill everything.
                    if(shutdown.get()) {
                        localDevice?.disconnect()
                        break
                    }
                }

                while(connected.get() && !shutdown.get()) {
                    // while we're connected, keep reading data.
                    val data = localDevice!!.read()

                    data ?: continue

                    liveData.postValue(data.joinToString("") { "%02X".format(it) })
                    Log.d("RFID", "Read data ${data.joinToString(" ") { "%02X".format(it)  }}")
                }

                // Sleep 500ms to avoid large reconnect burden
                serviceThreadSignal.tryAcquire(500, TimeUnit.MILLISECONDS)
            }

            if(shutdown.get()) {
                setState(State.DISABLED)
            }

            serviceThread.set(null)
        })
    }

    fun stop(join: Boolean = false) {
        shutdown.compareAndSet(false, true)

        // Force a disconnect on the device if we're connected.
        serviceThreadSignal.release()
        if(connected.get()) {
            device.get()?.disconnect()
        }

        if(join) {
            serviceThread.get()?.join()
        }

        setState(State.DISABLED)
    }

    private fun setState(newState: State){
        val oldState = state.getAndSet(newState)
        if(oldState != newState) {
            liveState.postValue(newState)
        }
    }

    internal fun onConnected() {
        setState(State.CONNECTED)
        connected.set(true)
    }

    internal fun onConnectError() {
        connected.set(false)
    }

    internal fun onDisconnect() {
        connected.compareAndSet(true, false)
    }
}