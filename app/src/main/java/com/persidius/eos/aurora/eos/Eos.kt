package com.persidius.eos.aurora.eos

import android.util.Log
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.rx2.rxMutate
import com.apollographql.apollo.rx2.rxQuery
import com.auth0.android.jwt.JWT
import com.persidius.eos.aurora.*
import com.persidius.eos.aurora.authorization.AuthorizationManager
import com.persidius.eos.aurora.type.CustomType
import com.persidius.eos.aurora.type.RecipientInput
import com.persidius.eos.aurora.type.TaskInput
import com.persidius.eos.aurora.util.Optional
import com.persidius.eos.aurora.util.Preferences
import io.reactivex.Single
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

const val COUNTY_CHUNK_SIZE = 2
const val UAT_CHUNK_SIZE = 30
const val LOC_CHUNK_SIZE = 50

object Eos {
    private var client = BehaviorSubject.create<ApolloClient>()

    fun init(am: AuthorizationManager) {
        // we need the AM so we can grab its token variable.
        // with that token we can change the Authorization header
        am.tokenObservable.withLatestFrom(Preferences.eosEnv)
            .map { v -> createApolloClient(v.first, v.second) }
            .subscribe(
                { v -> client.onNext(v) },
                { t -> client.onError(t) },
                { client.onComplete() }
            )
    }

    // This should hook onto the Auth Manager AND
    // the the shared pref observable.
    // and emit a new client each time the AM header changes.
    private fun createApolloClient(token: Optional<JWT>, serverDomain: String): ApolloClient {
        Log.d("EOS", "Create client w/ token ${if (token.isPresent()) token.get() else null}, domain $serverDomain")
        val httpClient = OkHttpClient.Builder()

        if (token.isPresent()) {
            // add the authorization header
            httpClient.addInterceptor { chain ->
                val req = chain.request()
                val builder = req.newBuilder().method(req.method(), req.body())
                builder.header("Authorization", "Bearer ${token.get()}")
                chain.proceed(builder.build())
            }
        }

        if (BuildConfig.ENABLE_HTTP_LOGGING) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.NONE
            httpClient.addInterceptor(interceptor)
        }

        val serverUrl = if (serverDomain.endsWith("persidius.dev")) "https://d-eos-graph.$serverDomain" else "https://eos-graph.$serverDomain"
        val apolloClient = ApolloClient.builder()
            .serverUrl(serverUrl)
            .okHttpClient(httpClient.build())
            .addCustomTypeAdapter(CustomType.TIMESTAMP, TimestampScalarType)

        return apolloClient.build()
    }

    fun queryCounties(): Single<Response<CountiesQuery.Data>> {
        return client.value!!.rxQuery(CountiesQuery())
            .subscribeOn(Schedulers.io())
            .firstOrError()
    }

    fun queryUats(countyIds: List<Int>): Single<Response<UatsQuery.Data>> {
        return client.value!!.rxQuery(UatsQuery(countyIds))
            .subscribeOn(Schedulers.io())
            .firstOrError()
    }

    fun queryLocs(uatIds: List<Int>): Single<Response<LocsQuery.Data>> {
        return client.value!!.rxQuery(LocsQuery(uatIds))
            .subscribeOn(Schedulers.io())
            .firstOrError()
    }

    fun queryArteries(locIds: List<Int>): Single<Response<ArteriesQuery.Data>> {
        return client.value!!.rxQuery(ArteriesQuery(locIds))
            .subscribeOn(Schedulers.io())
            .firstOrError()
    }

    fun queryRecipients(withTags: Boolean = false, updatedAfter: Int?, pageAfter: String? = null): Single<Response<RecipientsQuery.Data>> {
        return client.value!!.rxQuery(RecipientsQuery(withTags, Input.fromNullable(updatedAfter), Input.fromNullable(pageAfter)))
            .subscribeOn(Schedulers.io())
            .firstOrError()
    }

    fun queryTasks(updatedAfter: Int? = null, pageAfter: Int? = null): Single<Response<TaskSearchQuery.Data>> {
        return client.value!!.rxQuery(TaskSearchQuery(Input.fromNullable(updatedAfter), Input.fromNullable(pageAfter)))
            .subscribeOn(Schedulers.io())
            .firstOrError()
    }

    fun queryUsers(updatedAfter: Int?, pageAfter: String? = null): Single<Response<UsersQuery.Data>> {
        return client.value!!.rxQuery(UsersQuery(Input.fromNullable(updatedAfter), Input.fromNullable(pageAfter)))
            .subscribeOn(Schedulers.io())
            .firstOrError()
    }

    fun queryGroups(updatedAfter: Int?, pageAfter: String? = null): Single<Response<GroupsQuery.Data>> {
        return client.value!!.rxQuery(GroupsQuery(Input.fromNullable(updatedAfter), Input.fromNullable(pageAfter)))
            .subscribeOn(Schedulers.io())
            .firstOrError()
    }

    fun queryRecLabels(): Single<Response<RecLabelsQuery.Data>> {
        return client.value!!.rxQuery(RecLabelsQuery())
            .subscribeOn(Schedulers.io())
            .firstOrError()
    }

    fun editRecipient(id: String, createdAt: Int, recipient: RecipientInput): Single<Response<EditRecipientMutation.Data>> {
        return client.value!!.rxMutate(EditRecipientMutation(
            createdAt, id, recipient
        )).subscribeOn(Schedulers.io())
    }

    fun updateTask(gid: String, id: Int, updatedAt: Int, task: TaskInput): Single<Response<UpdateTaskMutation.Data>> {
        return client.value!!.rxMutate(UpdateTaskMutation(
            Input.fromNullable(gid), id, updatedAt, task
        )).subscribeOn(Schedulers.io())
    }
}