package com.persidius.eos.aurora.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.persidius.eos.aurora.bluetooth.devices.LFReader
import com.persidius.eos.aurora.bluetooth.devices.LFReaderType
import com.persidius.eos.aurora.bluetooth.devices.MultipenLF
import com.persidius.eos.aurora.bluetooth.devices.MultipenUHF
import com.persidius.eos.aurora.util.Preferences
import com.persidius.eos.aurora.util.asLiveData
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class BTService(private val appContext: Context): BroadcastReceiver() {
    companion object {
        val BLUETOOTH_SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    val tag = "BT_SVC"

    enum class State (val i: Int) {
        BT_DISABLED(0),     // Device is disabled.
        DISABLED(1),        //
        ENABLED(2),         // enabled but not configured.
        CONNECTING(3),      // attempting to connect to target device
        CONNECTED(4)        // Ready to go
    }


    private val state: BehaviorSubject<State> = BehaviorSubject.createDefault(State.BT_DISABLED)
    private val _tags: PublishSubject<String> = PublishSubject.create()
    val tags: Observable<String> = _tags
    val tagLiveData = _tags.asLiveData()
    val stateLiveData = state.asLiveData()

    // locked w/ 'lock'
    private var selectedDeviceId: String? = Preferences.btDeviceType.value
    private var selectedDeviceType: String? = Preferences.btDeviceId.value

    // locked w/ 'lock'
    private val lock = ReentrantLock()
    private val adapter: BluetoothAdapter? = (appContext.getSystemService((Context.BLUETOOTH_SERVICE)) as BluetoothManager?)?.adapter

    private var device: BluetoothDevice? = null
    private var socket: BluetoothSocket? = null

    private var connectSubscription: Disposable? = null
    private val disconnecting = AtomicBoolean(false)

    // Used to suppress exceptions that would normally generate 'disconnect' or 'fail' events.
    private val suppressExceptions: AtomicBoolean = AtomicBoolean(false)

    init {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        appContext.registerReceiver(this, filter)

        val initState = if(isAdapterEnabled()) { if(Preferences.btEnabled.value!!) { State.ENABLED } else { State.DISABLED}  } else { State.BT_DISABLED }
        state.onNext(initState)
        Log.d(tag, "Initial sate is ${state.value?.name}")

        // Preferences triggers
        Preferences.btDeviceId.subscribe { deviceId ->
            selectDevice(deviceId)
        }
        Preferences.btDeviceType.subscribe { deviceType ->
            setDeviceType(deviceType)
        }
        Preferences.btEnabled.subscribe { enabled ->
            setEnabled(enabled)
        }
    }

    private fun isConfigured() = selectedDeviceId in getDeviceList().map { it.id } && selectedDeviceType in getDeviceTypeList()

    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                // we only care about *disabled* and *enabled* states
                when(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)) {
                    BluetoothAdapter.STATE_ON -> {
                        lock.withLock {
                            if(state.value!! == State.BT_DISABLED) {
                                setEnabled(true)
                            }
                        }
                    }

                    // If we're connected, stop/start
                    BluetoothAdapter.STATE_TURNING_OFF -> {
                        lock.withLock {
                            if(state.value != State.DISABLED) {
                                setState(State.BT_DISABLED)
                                disconnect()
                            }
                        }
                    }
                }
            }
        }
    }

    fun getState(): Observable<State> = state

    private fun isAdapterEnabled(): Boolean {
        return adapter?.isEnabled ?: false
    }

    private fun setEnabled(enabled: Boolean = true) = lock.withLock {
        if(enabled) {
            if(!isAdapterEnabled()) {
                // request adapter enable
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                appContext.startActivity(intent)
            } else {
                setState(State.ENABLED)
                Log.d(tag, "isConfigured = ${isConfigured()}")
                if(isConfigured()) {
                    Log.d(tag, "Call connect()")
                    connect(selectedDeviceId!!)
                }
            }
        } else {
            disconnect()
            setState(State.DISABLED)
        }
    }

    data class ListDevice(val id: String, val name: String)
    fun getDeviceList(): List<ListDevice> {
        val devices = mutableListOf<ListDevice>()
        for(dev in adapter?.bondedDevices ?: listOf<BluetoothDevice>()) {
            devices.add(ListDevice(dev.address, dev.name))
        }
        return devices
    }

    fun getSelectedDeviceId(): String? = lock.withLock {
        return selectedDeviceId
    }

    fun getDeviceTypeList(): List<String> = listOf(
        MultipenUHF.name,
        MultipenLF.name,
        LFReader.typeAName,
        LFReader.typeBName
    )

    private fun deviceTypeInstance(type: String?): BTDeviceClass? {
        return when(type) {
            MultipenUHF.name -> MultipenUHF()
            MultipenLF.name -> MultipenLF()
            LFReader.typeAName -> LFReader(LFReaderType.TypeA)
            LFReader.typeBName -> LFReader(LFReaderType.TypeB)
            else -> null
        }
    }

    private fun setDeviceType(type: String) = lock.withLock {
        if(type !in getDeviceTypeList()) {
            return
        }

        if (selectedDeviceType == null || selectedDeviceType == type) {
            return
        }

        // if currentId != null, then stop connecting service & read thread.
        if (selectedDeviceType != null) {
            disconnect()
        }

        selectedDeviceType = type
        if (isConfigured() && (state.value == State.ENABLED || state.value == State.CONNECTING || state.value == State.CONNECTED)) {
            connect(selectedDeviceId!!)
        }
    }

    fun getSelectedDeviceType(): String? = lock.withLock {
        return selectedDeviceType
    }

    private fun setState(newState: State) = lock.withLock {
        if(newState != state.value) {
            Log.d(tag, "Enter new state ${newState.name}")
            state.onNext(newState)
        }
    }

    // Returns when completing.
    // Keeps looping forever or until
    // a signal is sent.

    private fun tryConnect(): Completable = Completable.create {emitter ->
        while(!disconnecting.get()) {
            try {
                Log.d(tag, "Connect loop...")
                socket = device?.createRfcommSocketToServiceRecord(BLUETOOTH_SPP_UUID)

                if(device == null || socket == null) {
                    Log.d(tag, "Connect loop null ref. Waiting 200ms and retrying")
                    Thread.sleep(200)
                    continue
                }

                if(socket!!.isConnected) {
                    Log.d(tag, "Socket is already connected")
                    break
                }

                socket!!.connect()
                if(socket!!.isConnected) {
                    Log.d(tag, "Connected successfully")
                    break
                }
            } catch(t: Throwable) {
                Log.d(tag, "Connect failed with exception $t")
            }
        }
        emitter.onComplete()
    }.subscribeOn(Schedulers.io())

    /**
     * Flag used to suppress errors inside read/write loops that would
     * normally trigger a disconnect action. This flag gets set once an
     * exception is raised that disconnects the device (or external user
     * action) is taken.
     */

    fun write(data: ByteArray): Completable = Completable.create{ emitter ->
        try {
            Log.d(tag, "Writing data ${data.map { it.toString(16) }}")
            socket?.outputStream!!.write(data)
            emitter.onComplete()
        } catch(t: Throwable) {
            emitter.onError(t)
        }
    }
    .subscribeOn(Schedulers.io())

    private fun read(): Observable<ByteArray> = Observable.create<ByteArray> { emitter ->
        try {
            while (socket != null) {
                val readSize = 64
                val data = ByteArray(readSize) { 0 }
                val sizeRead = socket?.inputStream?.read(data) ?: 0
                if(sizeRead != 0) {
                    emitter.onNext(data.sliceArray(IntRange(start = 0, endInclusive = sizeRead - 1)))
                }
            }
        } catch (t: Throwable) {
            if(!disconnecting.get() && !suppressExceptions.get()) {
                disconnect()
                connect(selectedDeviceId!!)
            }
            emitter.onComplete()
        }
    }.subscribeOn(Schedulers.io())

    /**
     * NOTE: Must be called with 'lock'
     */

    private var devClass: BTDeviceClass? = null

    private fun connect(id: String) = lock.withLock {
        if(connectSubscription != null) {
            return
        }
        
        Log.d(tag, "Starting connect thread")
        disconnecting.set(false)
        suppressExceptions.set(false)
        device = adapter?.getRemoteDevice(id)

        val completable = tryConnect()
        setState(State.CONNECTING)
        connectSubscription = completable.doFinally {
                lock.withLock {
                    connectSubscription = null
                }
            }.subscribe({
            if(!disconnecting.get()) {
                // we're now connected.
                // do the needful
                lock.withLock {
                    setState(State.CONNECTED)
                    devClass = deviceTypeInstance(selectedDeviceType)
                    devClass?.start(read(), this::write, _tags)
                }
            }
        }, {
            // we fcked up, but should we retry?
            // TODO: YES.
        })

        // don't do anything.
        if(device == null) {
            Log.d(tag, "Device = null. wtf?")
            return
        }
    }

    /**
     * NOTE: This method *DOES NOT* trigger the 'onDisconnect' callback.
     * It is up to the user to migrate to an appropriate which will trigger it.
     * NOTE2: Must be called with 'lock'
     */
    private fun disconnect() = lock.withLock {
        suppressExceptions.set(true)
        disconnecting.set(true)

        socket?.close()
        socket = null
        connectSubscription?.dispose()
        connectSubscription = null
        devClass?.dispose()
        devClass = null
    }

    fun selectDevice(id: String?) = lock.withLock {
        if ((selectedDeviceId == null && id == null) ||
            (selectedDeviceId != null && id != null && selectedDeviceId == id)) {
            return
        }

        // if currentId != null, then stop connecting service & read thread.
        if (selectedDeviceId != null) {
            disconnect()

            if(id == null) {
                setState(State.ENABLED)
            }
        }

        selectedDeviceId = id
        if (id != null && (state.value == State.ENABLED || state.value == State.CONNECTING || state.value == State.CONNECTED)) {
            connect(id)
        }
    }
}