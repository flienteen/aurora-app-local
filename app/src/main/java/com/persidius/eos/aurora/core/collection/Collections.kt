package com.persidius.eos.aurora.core.collection

import com.persidius.eos.aurora.database.dao.CollectionDao
import com.persidius.eos.aurora.database.dao.VehicleDao
import com.persidius.eos.aurora.database.entities.Collection
import com.persidius.eos.aurora.util.LocationProvider
import io.reactivex.Completable
import io.reactivex.Single
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.util.*

class Collections(private val collectionDao: CollectionDao, private val vehicleDao: VehicleDao, private val settings: CollectionsSettings, private val location: LocationProvider) {
  private fun epochNow() = Instant.now().epochSecond

  private fun hasPreviousCollection(tagId: String): Single<Boolean> {
    val minimumCreatedAt = epochNow() - settings.minimumTimeBetweenCollections.value!!
    val dbResult = collectionDao.findRecentForTag(tagId, minimumCreatedAt)
    return dbResult.map { it.isNotEmpty() }.toSingle()
  }

  fun recordCollection(tagId: String, vehicleLicensePlate: String): Completable {
    return Completable.fromCallable {
      if (hasPreviousCollection(tagId).blockingGet()) {
        throw DuplicateCollectionError("There is already a recent collection for this tag")
      }

      val vehicleResults = runBlocking { vehicleDao.find(vehicleLicensePlate) }
      val vehicle = if(vehicleResults.size == 1) { vehicleResults.first() } else null

      val collection = Collection(
        id = 0,
        posLat = location.lat,
        posLng = location.lng,
        createdAt = Instant.now().epochSecond,
        uploaded = false,
        extId = UUID.randomUUID().toString(),
        vehicleLicensePlate = vehicleLicensePlate,
        tag = tagId,
        countyId = vehicle?.countyId
      )
      collectionDao.insert(collection).blockingGet()
    }
  }
}
