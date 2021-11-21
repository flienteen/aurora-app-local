package com.persidius.eos.aurora.core.collection

import io.reactivex.subjects.BehaviorSubject

interface CollectionsSettings {
  val vehicleLicensePlate: BehaviorSubject<String>
  val minimumTimeBetweenCollections: BehaviorSubject<Int>
}