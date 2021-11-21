package com.persidius.eos.aurora.core.collections

import com.persidius.eos.aurora.core.collection.Collections
import com.persidius.eos.aurora.core.collection.CollectionsSettings
import com.persidius.eos.aurora.core.collection.DuplicateCollectionError
import com.persidius.eos.aurora.database.dao.CollectionDao
import com.persidius.eos.aurora.database.dao.VehicleDao
import com.persidius.eos.aurora.database.entities.Collection
import com.persidius.eos.aurora.database.entities.Vehicle
import com.persidius.eos.aurora.util.LocationProvider
import io.mockk.*
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.exceptions.CompositeException
import io.reactivex.subjects.BehaviorSubject
import org.junit.After
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import java.lang.RuntimeException
import java.time.Instant

class CollectionsTest {

  companion object {
    private const val TEST_LICENSE_PLATE = "B-181-TST"
    private const val TEST_LICENSE_PLATE_NOT_REGISTERED = "B-292-TST"
    private const val TEST_LAT = 24.33
    private const val TEST_LNG = 45.66
    private const val TEST_TAG = "A9F3B2CC88"
    private const val TEST_TIME_BETWEEN_COLLECTIONS = 600
    private const val TEST_VEHICLE_COUNTY = 98
    private lateinit var collections: Collections
    private lateinit var settings: CollectionsSettings
    private lateinit var dao: CollectionDao
    private lateinit var location: LocationProvider
    private lateinit var vehicleDao: VehicleDao

    @BeforeClass
    @JvmStatic
    fun setup() {
      dao = mockk(relaxed = true)
      vehicleDao = mockk(relaxed = true)
      settings = mockk(relaxed = true)
      location = mockk(relaxed = true)
      collections = Collections(dao, vehicleDao, settings, location)
    }
  }

  @After
  fun cleanup() {
    unmockkAll()
  }

  @Test
  fun createsCollectionSuccessfully() {
    val coll = slot<Collection>()
    every { settings.vehicleLicensePlate } returns BehaviorSubject.createDefault(TEST_LICENSE_PLATE)
    every { dao.findRecentForTag(any(), any()) } returns Maybe.just(listOf())
    every { dao.insert(capture(coll)) } returns Completable.complete()
    every { location.lat } returns TEST_LAT
    every { location.lng } returns TEST_LNG
    coEvery { vehicleDao.find(TEST_LICENSE_PLATE) }  returns listOf(Vehicle(id = 1, vehicleLicensePlate = TEST_LICENSE_PLATE, countyId = TEST_VEHICLE_COUNTY))

    collections.recordCollection(TEST_TAG, TEST_LICENSE_PLATE).blockingAwait()
    Assert.assertTrue(coll.isCaptured)
    val timeDelta = kotlin.math.abs(coll.captured.createdAt - Instant.now().epochSecond)
    Assert.assertTrue(timeDelta < 2)
    Assert.assertEquals(false, coll.captured.uploaded)
    Assert.assertEquals(TEST_TAG, coll.captured.tag)
    Assert.assertEquals(TEST_LICENSE_PLATE, coll.captured.vehicleLicensePlate)
    Assert.assertEquals(TEST_LAT, coll.captured.posLat, 0.0)
    Assert.assertEquals(TEST_LNG, coll.captured.posLng, 0.0)
    Assert.assertEquals(TEST_VEHICLE_COUNTY, coll.captured.countyId)


    collections.recordCollection(TEST_TAG, TEST_LICENSE_PLATE_NOT_REGISTERED).blockingAwait()
    Assert.assertEquals(false, coll.captured.uploaded)
    Assert.assertEquals(TEST_TAG, coll.captured.tag)
    Assert.assertEquals(TEST_LICENSE_PLATE_NOT_REGISTERED, coll.captured.vehicleLicensePlate)
    Assert.assertEquals(TEST_LAT, coll.captured.posLat, 0.0)
    Assert.assertEquals(TEST_LNG, coll.captured.posLng, 0.0)
    Assert.assertEquals(null, coll.captured.countyId)
  }

  @Test
  fun refusesToCreateDuplicateCollection() {
    every { dao.findRecentForTag(TEST_TAG, any()) } returns Maybe.just(listOf(Collection(
      id = 1,
      tag = TEST_TAG,
      vehicleLicensePlate = TEST_LICENSE_PLATE,
      uploaded = false,
      extId = "84ebed67-bbe4-4d53-a641-5aef84fe5171",
      createdAt = Instant.now().epochSecond - 10,
      posLat = TEST_LAT,
      posLng = TEST_LNG,
      countyId = TEST_VEHICLE_COUNTY
    )))
    every { settings.vehicleLicensePlate } returns BehaviorSubject.createDefault(TEST_LICENSE_PLATE)
    every { settings.minimumTimeBetweenCollections } returns BehaviorSubject.createDefault(TEST_TIME_BETWEEN_COLLECTIONS)

    try {
      collections.recordCollection(TEST_TAG, TEST_LICENSE_PLATE).blockingAwait()
      Assert.fail("Expected throw")
    } catch(e: Exception) {
      Assert.assertTrue(e is RuntimeException)
      Assert.assertTrue(e.cause is DuplicateCollectionError)
    }
  }
}