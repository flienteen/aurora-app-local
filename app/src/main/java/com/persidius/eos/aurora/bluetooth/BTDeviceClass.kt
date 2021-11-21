package com.persidius.eos.aurora.bluetooth

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

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