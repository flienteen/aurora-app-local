package com.persidius.eos.aurora.bluetooth.devices

import android.annotation.SuppressLint
import android.util.Log
import com.persidius.eos.aurora.bluetooth.BTDeviceClass
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class MultipenUHF: BTDeviceClass {
    companion object {
        const val name = "Multipen UHF"
        private const val PKT_SEP = '\r'
    }

    @SuppressLint("CheckResult")
    override fun start(read: Observable<ByteArray>, write: (data: ByteArray) -> Completable, tags: BehaviorSubject<String>) {

        // Multipen UHF class, expects tags that are

        // start the multipen stuff.
        // TODO: Clean up memory, set to LF&UHF mode
        Log.d("Multipen", "Start")
        var buffer = ""
        read.subscribe {
            buffer += it.joinToString(separator="") { it.toChar().toString() }
            while(PKT_SEP in buffer) {
                val ix = buffer.indexOf(PKT_SEP)
                val packet = buffer.slice(IntRange(0, ix))
                buffer = buffer.slice(IntRange(ix + 1, buffer.length - 1))

                Log.d("Multipen", "Packet=$packet")
            }
        }
    }
}