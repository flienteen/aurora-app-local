package com.persidius.eos.aurora.eos

import android.util.Log
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.rx2.rxQuery
import com.auth0.android.jwt.JWT
import com.persidius.eos.aurora.*
import com.persidius.eos.aurora.authorization.AuthorizationManager
import com.persidius.eos.aurora.util.Optional
import com.persidius.eos.aurora.util.Preferences
import io.reactivex.Observable
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
            .subscribe (
                { v -> client.onNext(v) },
                { t -> client.onError(t) },
                { client.onComplete() }
            )
    }

    // This should hook onto the Auth Manager AND
    // the the shared pref observable.
    // and emit a new client each time the AM header changes.
    private fun createApolloClient(token: Optional<JWT>, serverDomain: String): ApolloClient {
        Log.d("EOS", "Create client w/ token ${if(token.isPresent()) token.get() else null}, domain $serverDomain")
        val httpClient = OkHttpClient.Builder()

        if(token.isPresent()) {
            // add the authorization header
            httpClient.addInterceptor { chain ->
                val req = chain.request()
                val builder = req.newBuilder().method(req.method(), req.body())
                builder.header("Authorization", "Bearer ${token.get()}")
                chain.proceed(builder.build())
            }
        }

        if(BuildConfig.ENABLE_HTTP_LOGGING) {
            httpClient.addInterceptor(HttpLoggingInterceptor())
        }

        val apolloClient = ApolloClient.builder()
            .serverUrl("https://eos-graph.$serverDomain")
            .okHttpClient(httpClient.build())
            // .addCustomTypeAdapter(CustomType.Timestamp, TimestampScalarType)

        return apolloClient.build()
    }

    fun queryCounties(): Observable<Response<CountiesQuery.Data>> {
        return client.value!!.rxQuery(CountiesQuery()).observeOn(Schedulers.io())
    }

    fun queryUats(countyIds: List<Int>): Observable<Response<UatsQuery.Data>> {
        return client.value!!.rxQuery(UatsQuery(countyIds)).observeOn(Schedulers.io())
    }

    fun queryLocs(uatIds: List<Int>): Observable<Response<LocsQuery.Data>> {
        return client.value!!.rxQuery(LocsQuery(uatIds)).observeOn(Schedulers.io())
    }

    fun queryArteries(locIds: List<Int>): Observable<Response<ArteriesQuery.Data>> {
        return client.value!!.rxQuery(ArteriesQuery(locIds)).observeOn(Schedulers.io())
    }

    fun queryRecipients(withTags: Boolean = false, updatedAfter: Int?): Observable<Response<RecipientsQuery.Data>> {
        return client.value!!.rxQuery(RecipientsQuery(withTags, Input.fromNullable(updatedAfter))).observeOn(Schedulers.io())
    }

    fun queryUsers(updatedAfter: Int?): Observable<Response<UsersQuery.Data>> {
        return client.value!!.rxQuery(UsersQuery(Input.fromNullable(updatedAfter))).observeOn(Schedulers.io())
    }

    fun queryGroups(updatedAfter: Int?): Observable<Response<GroupsQuery.Data>> {
        return client.value!!.rxQuery(GroupsQuery(Input.fromNullable(updatedAfter))).observeOn(Schedulers.io())
    }

    fun queryRecLabels(): Observable<Response<RecLabelsQuery.Data>> {
        return client.value!!.rxQuery(RecLabelsQuery()).observeOn(Schedulers.io())
    }
}