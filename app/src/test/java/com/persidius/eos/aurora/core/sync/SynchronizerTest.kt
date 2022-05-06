package com.persidius.eos.aurora.core.sync

import com.persidius.eos.aurora.database.EosDatabase
import com.persidius.eos.aurora.database.entities.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test

class SynchronizerTest {
  companion object {
    private lateinit var database: EosDatabase
    private lateinit var api: EosAPI
    private lateinit var config: SynchronizerConfiguration

    @BeforeClass
    @JvmStatic
    fun setup() {
      database = mockk(relaxed = true)
      api = mockk(relaxed = true)
      config = mockk(relaxed = true)
    }
  }

  @After
  fun after() {
    clearAllMocks()
  }

  @Test
  fun clearsDefinitions() {
    coEvery { database.locDao().deleteAll() } returns Unit
    coEvery { database.uatDao().deleteAll() } returns Unit
    coEvery { database.countyDao().deleteAll() } returns Unit
    coEvery { database.arteryDao().deleteAll() } returns Unit
    coEvery { database.recLabelDao().deleteAll() } returns Unit
    coEvery { database.vehicleDao().deleteAll() } returns Unit

    val synchro = Synchronizer(database, api, config)
    synchro.clearDefinitions()

    coVerify { database.countyDao().deleteAll() }
    coVerify { database.uatDao().deleteAll() }
    coVerify { database.locDao().deleteAll() }
    coVerify { database.arteryDao().deleteAll() }
    coVerify { database.vehicleDao().deleteAll() }
    coVerify { database.uatDao().deleteAll() }

  }

  @Test
  fun getDefinitions() {

    val testUatList = listOf(Uat(
      id = 2000,
      name = "Test Uat",
      countyId = 100
    ))
    val testCountyList = listOf(County(
      id = 100,
      name = "Test County",
      seq = 100,
      short_ = "TC"
    ))

    val testLocList = listOf(Loc(
      id = 30000,
      name = "Test Loc",
      uatId = 2000,
      countyId = 100
    ))

    val testArteryList = listOf(Artery(
      id = 400000,
      name = "Testului",
      prefix = "Strada",
      locId = 30000
    ))

    val testRecLabelList = listOf(RecommendedLabel(
      label = "test_label",
      displayName = "Eticheta Test"
    ))

    val testVehicleList = listOf(Vehicle(
      id = 0,
      vehicleLicensePlate = "B-181-TST",
      countyId = 32,
    ), Vehicle(
      id = 1,
      vehicleLicensePlate = "C-292-TST",
      countyId = 32,
    ))

    val includeVehicles = slot<Boolean>()
    coEvery { api.getDefinitions(capture(includeVehicles)) } returns
      EosAPI.DefinitionsResponse(
        counties = testCountyList,
        uats = testUatList,
        locs = testLocList,
        arteries = testArteryList,
        recommendedLabels = testRecLabelList,
        vehicles = testVehicleList
      )

    val uatList = slot<List<Uat>>()
    val locList = slot<List<Loc>>()
    val countyList = slot<List<County>>()
    val arteryList = slot<List<Artery>>()
    val recLabelList = slot<List<RecommendedLabel>>()
    val vehicleList = slot<List<Vehicle>>()

    coEvery { database.countyDao().insert(capture(countyList)) } returns Unit
    coEvery { database.uatDao().insert(capture(uatList)) } returns Unit
    coEvery { database.locDao().insert(capture(locList)) } returns Unit
    coEvery { database.arteryDao().insert(capture(arteryList)) } returns Unit
    coEvery { database.recLabelDao().insert(capture(recLabelList)) } returns Unit
    coEvery { database.vehicleDao().insert(capture(vehicleList)) } returns Unit

    every { config.synchronizeCollections } returns false

    val synchro = Synchronizer(database, api, config)
    synchro.getDefinitions()

    Assert.assertTrue(includeVehicles.isCaptured)
    Assert.assertFalse(includeVehicles.captured)

    Assert.assertEquals(testCountyList, countyList.captured)
    Assert.assertEquals(testUatList, uatList.captured)
    Assert.assertEquals(testLocList, locList.captured)
    Assert.assertEquals(testArteryList, arteryList.captured)
    Assert.assertEquals(testRecLabelList, recLabelList.captured)
    Assert.assertFalse(vehicleList.isCaptured)

    every { config.synchronizeCollections } returns true

    synchro.getDefinitions()
    Assert.assertTrue(includeVehicles.captured)
    Assert.assertEquals(testVehicleList, vehicleList.captured)
  }

  private fun mockRecipientResponse(): List<Recipient> {
    val testRecipient: Recipient = mockk(relaxed = true)
    val pageNumber = slot<Int>()
    val totalTestItems = 3
    coEvery {
      api.getRecipientPage(capture(pageNumber))
    } answers {
      val items = if (pageNumber.captured < totalTestItems) {
        listOf(testRecipient.copy(eosId = pageNumber.captured.toString()))
      } else {
        listOf()
      }

      EosAPI.RecipientPageResponse(
        pageNumber = pageNumber.captured,
        totalItems = totalTestItems,
        items = items
      )
    }

    return (0 until totalTestItems).map { testRecipient.copy(eosId = it.toString()) }
  }

  @Test
  fun paginateAndSave() {
    mockRecipientResponse()

    runBlocking {
      Synchronizer.paginateAndSave({api.getRecipientPage(it)}, {Assert.assertTrue(it.isNotEmpty())})
    }

    coVerifySequence {
      api.getRecipientPage(0)
      api.getRecipientPage(1)
      api.getRecipientPage(2)
      api.getRecipientPage(3)
    }
  }

  @Test
  fun testGetRecipients() {
    val recipientList = mutableListOf<Recipient>()
    val recipientSlot = slot<List<Recipient>>()
    coEvery { database.recipientDao().insert(capture(recipientSlot)) } answers {
      recipientList.addAll(recipientSlot.captured)
      Unit
    }

    val testRecipients = mockRecipientResponse()

    val synchro = Synchronizer(database, api, config)
    synchro.getRecipients()

    Assert.assertEquals(testRecipients, recipientList)
  }

  private fun mockGroupResponse(): List<Group> {
    val testGroup: Group = mockk(relaxed = true)
    val pageNumber = slot<Int>()
    val totalTestItems = 3
    coEvery {
      api.getGroupPage(capture(pageNumber))
    } answers {
      val items = if (pageNumber.captured < totalTestItems) {
        listOf(testGroup.copy(eosId = pageNumber.captured.toString()))
      } else {
        listOf()
      }

      EosAPI.GroupPageResponse(
        pageNumber = pageNumber.captured,
        totalItems = totalTestItems,
        items = items
      )
    }

    return (0 until totalTestItems).map { testGroup.copy(eosId = it.toString()) }
  }

  @Test
  fun testGetGroups() {
    val groupList = mutableListOf<Group>()
    val groupSlot = slot<List<Group>>()
    coEvery { database.groupDao().insert(capture(groupSlot)) } answers {
      groupList.addAll(groupSlot.captured)
      Unit
    }

    val testGroups = mockGroupResponse()

    val synchro = Synchronizer(database, api, config)
    synchro.getGroups()

    Assert.assertEquals(testGroups, groupList)
  }

  private fun mockTagResponse(): List<RecipientTag> {
    val testTag: RecipientTag = mockk(relaxed = true)
    val pageNumber = slot<Int>()
    val totalTestItems = 3
    coEvery {
      api.getRecipientTagPage(capture(pageNumber))
    } answers {
      val items = if (pageNumber.captured < totalTestItems) {
        listOf(testTag.copy(tag = pageNumber.captured.toString(), recipientId = pageNumber.captured.toString()))
      } else {
        listOf()
      }

      EosAPI.RecipientTagPageResponse(
        pageNumber = pageNumber.captured,
        totalItems = totalTestItems,
        items = items
      )
    }

    return (0 until totalTestItems).map { testTag.copy(tag = it.toString(), recipientId = it.toString()) }
  }

  @Test
  fun testGetRecipientTags() {
    val tagList = mutableListOf<RecipientTag>()
    val tagSlot = slot<List<RecipientTag>>()

    coEvery { database.recipientTagDao().insert(capture(tagSlot)) } answers {
      tagList.addAll(tagSlot.captured)
      Unit
    }

    val testTags = mockTagResponse()

    val synchro = Synchronizer(database, api, config)
    synchro.getRecipientTags()

    Assert.assertEquals(testTags, tagList)
  }

  @Test
  fun deltaUpdateRecipients() {

  }
  // deltaUpdateGroups
  // deltaUpdateTags

  // uploadTagUpdates
  // uploadRecipientUpdates
  // uploadCollections

}