package com.persidius.eos.aurora.eos

import android.util.Log
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.rx2.rxMutate
import com.apollographql.apollo.rx2.rxQuery
import com.auth0.android.jwt.JWT
import com.persidius.eos.aurora.*
import com.persidius.eos.aurora.auth.AuthManager
import com.persidius.eos.aurora.database.entities.Collection
import com.persidius.eos.aurora.database.entities.RecipientTagUpdate
import com.persidius.eos.aurora.database.entities.RecipientUpdate
import com.persidius.eos.aurora.type.*
import com.persidius.eos.aurora.util.DateUtil
import com.persidius.eos.aurora.util.Optional
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class EosGraphQL(am: AuthManager) {
  private var client = BehaviorSubject.create<ApolloClient>()

  companion object {
    const val RECIPIENT_PAGE_SIZE = 3000
    const val GROUP_PAGE_SIZE = 3000
    const val RECIPIENT_TAG_PAGE_SIZE = 3000
  }

  init {
    am.session.jwt.map {createApolloClient(it, BuildConfig.EOS_GRAPHQL_URL)}
        .subscribe(
            {v -> client.onNext(v) },
            {t -> client.onError(t) },
            { client.onComplete() }
        )
  }

  // This should hook onto the Auth Manager AND
  // the the shared pref observable.
  // and emit a new client each time the AM header changes.
  private fun createApolloClient(token: Optional<JWT>, serverUrl: String): ApolloClient {
    Log.d("EOS", "Create client w/ token ${if (token.isPresent()) token.get() else null}, url $serverUrl")
    val httpClient = OkHttpClient.Builder()

    if (token.isPresent()) {
        // add the authorization header
        httpClient.addInterceptor { chain ->
            val req = chain.request()
            val builder = req.newBuilder().method(req.method, req.body)
            builder.header("Authorization", "Bearer ${token.get()}")
            chain.proceed(builder.build())
        }
    }

    if (BuildConfig.ENABLE_HTTP_LOGGING) {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        httpClient.addInterceptor(interceptor)
    }

    val apolloClient = ApolloClient.builder()
        .serverUrl(serverUrl)
        .addCustomTypeAdapter(CustomType.DATETIME, DateTimeAdapter)
        .okHttpClient(httpClient.build())

    return apolloClient.build()
  }

  fun queryDefinitions(includeVehicles: Boolean): Single<Response<DefinitionsQuery.Data>> {
    return client.value!!.rxQuery(DefinitionsQuery(includeVehicles))
      .subscribeOn(Schedulers.io())
      .firstOrError()
  }

  fun queryRecipients(pageNumber: Int): Single<Response<RecipientsQuery.Data>> {
    return client.value!!.rxQuery(RecipientsQuery(RECIPIENT_PAGE_SIZE, Input.fromNullable(pageNumber), Input.absent()))
      .subscribeOn(Schedulers.io())
      .firstOrError()
  }

  fun deltaQueryRecipients(afterId: Int, pageNumber: Int?): Single<Response<RecipientsQuery.Data>> {
    return client.value!!.rxQuery(RecipientsQuery(RECIPIENT_PAGE_SIZE, Input.fromNullable(pageNumber), Input.fromNullable(afterId)))
      .subscribeOn(Schedulers.io())
      .firstOrError()
  }

  fun queryGroups(pageNumber: Int?): Single<Response<GroupsQuery.Data>> {
    return client.value!!.rxQuery(GroupsQuery(GROUP_PAGE_SIZE, Input.fromNullable(pageNumber), Input.absent()))
      .subscribeOn(Schedulers.io())
      .firstOrError()
  }

  fun deltaQueryGroups(afterId: Int, pageNumber: Int?): Single<Response<GroupsQuery.Data>> {
    return client.value!!.rxQuery(GroupsQuery(GROUP_PAGE_SIZE, Input.fromNullable(pageNumber), Input.fromNullable(afterId)))
      .subscribeOn(Schedulers.io())
      .firstOrError()
  }

  fun queryRecipientTags(pageNumber: Int?): Single<Response<RecipientTagsQuery.Data>> {
    return client.value!!.rxQuery(RecipientTagsQuery(RECIPIENT_TAG_PAGE_SIZE, Input.fromNullable(pageNumber), Input.absent()))
      .subscribeOn(Schedulers.io())
      .firstOrError()
  }

  fun deltaQueryRecipientTags(afterId: Int, pageNumber: Int?): Single<Response<RecipientTagsQuery.Data>> {
    return client.value!!.rxQuery(RecipientTagsQuery(RECIPIENT_TAG_PAGE_SIZE, Input.fromNullable(pageNumber), Input.fromNullable(afterId)))
      .subscribeOn(Schedulers.io())
      .firstOrError()
  }

  private fun <T>absentFromNull(v: T?): Input<T> = if(v == null) Input.absent() else Input.fromNullable(v)

  fun updateRecipient(update: RecipientUpdate): Single<Response<UpdateRecipientMutation.Data>> {
    val input = RecipientUpdateInput(
      labels = if(update.labels == null) Input.absent() else Input.fromNullable(update.labels.entries.map { LabelInput(it.key, Input.fromNullable(it.value)) }.toList()),
      stream = if(update.stream == null) Input.absent() else Input.fromNullable(if(WasteStream.safeValueOf(update.stream) == WasteStream.UNKNOWN__) WasteStream.REZ else WasteStream.valueOf(update.stream)),
      groupId = if(update.groupId == null) Input.absent() else Input.fromNullable(if(update.groupId.isEmpty()) null else update.groupId),
      locId = absentFromNull(update.locId),
      uatId = absentFromNull(update.uatId),
      addressNumber = absentFromNull(update.addressNumber),
      addressStreet = absentFromNull(update.addressStreet),
      comments = absentFromNull(update.comments),
      size = absentFromNull(update.size),
      posLat = absentFromNull(update.posLat),
      posLng = absentFromNull(update.posLng),
      lifecycle = absentFromNull(if(update.lifecycle == null) { null } else { RecipientLifecycle.valueOf(update.lifecycle) })
    )
    return client.value!!.rxMutate(UpdateRecipientMutation(
      eosId = update.eosId,
      input = input)
    ).subscribeOn(Schedulers.io())
  }

  fun updateTag(update: RecipientTagUpdate): Single<Response<UpdateRecipientTagMutation.Data>> {
    val input = RecipientTagUpdateInput(
      slot = update.slot,
      recipientId = Input.fromNullable(update.recipientId)
    )
    return client.value!!.rxMutate(UpdateRecipientTagMutation(
      tag = update.tag,
      input = input
    )).subscribeOn(Schedulers.io())
  }

  fun createCollection(collection: Collection): Single<Response<CreateCollectionMutation.Data>> {
    val input = CollectionCreateInput(
      tag = collection.tag,
      extId = collection.extId,
      posLat = collection.posLat,
      posLng = collection.posLng,
      vehicleLicensePlate = collection.vehicleLicensePlate,
      countyId = absentFromNull(collection.countyId),
      source = CollectionSource.MANUAL,
      createdAt = DateUtil.unixToISOTimestamp(collection.createdAt),
      arrivedAt = DateUtil.nowISOTimestamp()
    )
    return client.value!!.rxMutate(CreateCollectionMutation(
      input = input
    ))
  }
}