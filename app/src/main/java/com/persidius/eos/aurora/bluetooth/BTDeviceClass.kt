package com.persidius.eos.aurora.bluetooth

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * The BT Device should store the socket? -> no.
 * It should act solely as an event listener for onRead and have a 'write' method
 * that it can use.
 *
 */
interface BTDeviceClass {
    /**
     * Initialise the device class
     */
    fun start(read: Observable<ByteArray>, write: (data: ByteArray) -> Completable, tags: PublishSubject<String>)

    /*
        For disposing of any subscriptions
     */
    fun dispose() { }
}