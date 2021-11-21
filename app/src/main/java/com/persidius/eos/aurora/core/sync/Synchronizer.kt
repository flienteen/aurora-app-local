package com.persidius.eos.aurora.core.sync

import com.persidius.eos.aurora.database.EosDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

class Synchronizer(val db: EosDatabase, val api: EosAPI, val config: SynchronizerConfiguration) {
  private val mutableStatus = MutableStateFlow("")
  val status get() = this.mutableStatus as StateFlow<String>

  private val includeVehicles get() = config.synchronizeCollections

  internal fun clearDefinitions() {
    runBlocking {
      db.arteryDao().deleteAll()
      db.locDao().deleteAll()
      db.uatDao().deleteAll()
      db.countyDao().deleteAll()
      db.vehicleDao().deleteAll()
      db.recLabelDao().deleteAll()
    }
  }

  internal fun getDefinitions() {
    val definitions = runBlocking { api.getDefinitions(includeVehicles = includeVehicles) }

    runBlocking {
      db.countyDao().insert(definitions.counties)
      db.uatDao().insert(definitions.uats)
      db.locDao().insert(definitions.locs)
      db.arteryDao().insert(definitions.arteries)
      db.recLabelDao().insert(definitions.recommendedLabels)
      if (includeVehicles) {
        db.vehicleDao().insert(definitions.vehicles)
      }
    }
  }

  internal fun getRecipients() {
    // todo: report progress
    runBlocking {
      paginateAndSave({ pageNumber -> api.getRecipientPage(pageNumber)}, {
        recipients -> db.recipientDao().insert(recipients)})
      }
  }

  internal fun getGroups() {
    runBlocking {
      paginateAndSave({pageNumber -> api.getGroupPage(pageNumber)}, {
        groups -> db.groupDao().insert(groups)
      })
    }
  }

  internal fun getRecipientTags() {
    runBlocking {
      paginateAndSave({pageNumber -> api.getRecipientTagPage(pageNumber)}, {
        tags -> db.recipientTagDao().insert(tags)
      })
    }
  }

  companion object {
    internal suspend fun <T> paginateAndSave(
      getPage: suspend (Int) -> EosAPI.PageResponse<T>,
      save: suspend (List<T>) -> Unit
    ) {
      var pageNumber = 0
      var reachedEnd = false;
      do {
        val page = getPage(pageNumber)

        if (page.items.isNotEmpty()) {
          save(page.items)
        } else {
          reachedEnd = true
        }

        ++pageNumber
      } while (!reachedEnd)
    }
  }
}