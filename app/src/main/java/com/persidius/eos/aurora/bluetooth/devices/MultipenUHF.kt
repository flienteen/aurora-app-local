package com.persidius.eos.aurora.bluetooth.devices

import android.annotation.SuppressLint
import android.util.Log
import com.persidius.eos.aurora.bluetooth.BTDeviceClass
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.*

class MultipenUHF: BTDeviceClass {
    companion object {
        private const val tag = "MultipenUHF"
        const val name = "Multipen UHF"
        private const val PKT_SEP = '\r'
    }

    private val subs: CompositeDisposable = CompositeDisposable()
    private var packets: PublishSubject<String> = PublishSubject.create()

    @SuppressLint("CheckResult")
    override fun start(read: Observable<ByteArray>, write: (data: ByteArray) -> Completable, tags: PublishSubject<String>) {
        val cmds = listOf(
            "me",               // Erase mem
            "cw 04,00",         // No poweroff
            "cw 0F,01",         // Bt on @ startup
            "cw 0E,01",         // bt spp echo
            "cw 0D,04",         // UHF Repeat 4x
            "cw 09,04",         // UHF only
            "cw 61,01",         // Set read to EPC
            "cw 62,01"          //
        )

        var buffer = ""
        val packets: PublishSubject<String> = PublishSubject.create()

        subs.add(read.subscribe {
            buffer += it.joinToString(separator="") { it.toChar().toString() }
            while(PKT_SEP in buffer) {
                val ix = buffer.indexOf(PKT_SEP)
                val packet = buffer.slice(IntRange(0, ix - 1))
                buffer = buffer.slice(IntRange(ix + 1, buffer.length - 1))

                packets.onNext(packet)
                // Tags always start with 'E2'
                if (packet.startsWith("E2")) {
                    tags.onNext(packet.toUpperCase(Locale.US).trim())
                }

                Log.d(tag, "Packet='$packet'")
            }
        })

        val disp = cmds.fold(null as Completable?) { c: Completable?, cmd ->
            Log.d(tag, "Writing cmd $cmd")
            val w = write("$cmd$PKT_SEP".toByteArray())
                .andThen(packets.firstElement().ignoreElement())
            if (c != null) c.andThen(w) else w
        }?.subscribe({ }, { t -> Log.d(tag, "Error while writing cmds", t) })

        if(disp != null) { subs.add(disp) }
    }

    override fun dispose() {
        Log.d(tag, "Disposing subscriptions")
        super.dispose()
        subs.dispose()
    }
}