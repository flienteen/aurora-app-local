package com.persidius.eos.aurora.bluetooth.devices

import android.util.Log
import com.persidius.eos.aurora.bluetooth.BTDeviceClass
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.io.IOException

class LFReader(private val type: LFReaderType) : BTDeviceClass {
    companion object {
        private const val tag = "LFReader"
        const val typeAName = "LF Reader A (negru)"
        const val typeBName = "LF Reader B (argintiu)"
    }

    private val subs = CompositeDisposable()

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { String.format("%02X", it) }
    }

    override fun start(read: Observable<ByteArray>, write: (data: ByteArray) -> Completable, tags: PublishSubject<String>) {
        val packetStart = when(type) {
            LFReaderType.TypeA -> "FF"
            LFReaderType.TypeB -> "AAFF"
        }
        val packetLength = when(type) {
            LFReaderType.TypeA -> 6*2
            LFReaderType.TypeB -> 8*2
        }
        var buffer = ""

        subs.add(read.subscribe {
            // if we receive an incomplete packet then buffer it.
            val packet = bytesToHex(it)
            Log.d(tag, "Received data = $packet")
            buffer += packet

            if (buffer.length < packetLength) {
                return@subscribe
            }

            while (buffer.length >= packetLength) {
                if (!buffer.startsWith(packetStart)) {
                    buffer = ""
                    Log.d(tag, "Packet stream desynchronized, expected buffer to start with $packetStart, instead got ${buffer.take(packetStart.length)}")
                }
                var decodedTag = buffer.take(packetLength)
                buffer = buffer.takeLast(buffer.length - packetLength)
                // strip tag of start
                decodedTag = decodedTag.takeLast(decodedTag.length - packetStart.length)

                // Strip tag of end
                if (type == LFReaderType.TypeB) {
                    decodedTag = decodedTag.take(decodedTag.length - 2)
                }

                tags.onNext(decodedTag)
                Log.d(tag, "Tag='$decodedTag'")
            }
        })
    }

    override fun dispose() {
        Log.d(tag, "Disposing subscriptions")
        super.dispose()
        subs.dispose()
    }
}