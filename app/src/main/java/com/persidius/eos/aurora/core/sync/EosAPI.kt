package com.persidius.eos.aurora.core.sync

import com.persidius.eos.aurora.database.entities.*

interface EosAPI {
  suspend fun getDefinitions(includeVehicles: Boolean): DefinitionsResponse
  data class DefinitionsResponse(
    val counties: List<County>,
    val uats: List<Uat>,
    val locs: List<Loc>,
    val arteries: List<Artery>,
    val vehicles: List<Vehicle>,
    val recommendedLabels: List<RecommendedLabel>,
  )

  interface PageResponse<T> {
    val totalItems: Int
    val pageNumber: Int
    val items: List<T>
  }

  suspend fun getRecipientPage(pageNumber: Int): RecipientPageResponse
  data class RecipientPageResponse(
    override val totalItems: Int,
    override val pageNumber: Int,
    override val items: List<Recipient>
  ): PageResponse<Recipient>

  suspend fun getGroupPage(pageNumber: Int): GroupPageResponse
  data class GroupPageResponse(
    override val totalItems: Int,
    override val pageNumber: Int,
    override val items: List<Group>
  ): PageResponse<Group>

  suspend fun getRecipientTagPage(pageNumber: Int): RecipientTagPageResponse
  data class RecipientTagPageResponse(
    override val totalItems: Int,
    override val pageNumber: Int,
    override val items: List<RecipientTag>
  ): PageResponse<RecipientTag>

}