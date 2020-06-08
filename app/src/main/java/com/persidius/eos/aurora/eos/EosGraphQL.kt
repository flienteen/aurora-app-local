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
import com.persidius.eos.aurora.type.CustomType
import com.persidius.eos.aurora.util.Optional
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class EosGraphQL(am: AuthManager) {
    private var client = BehaviorSubject.create<ApolloClient>()

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
                val builder = req.newBuilder().method(req.method(), req.body())
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
            .okHttpClient(httpClient.build())

        return apolloClient.build()
    }

    fun queryDefinitions(): Single<Response<DefinitionsQuery.Data>> {
        return client.value!!.rxQuery(DefinitionsQuery())
            .subscribeOn(Schedulers.io())
            .firstOrError()
    }

   /* fun queryRecipients(withTags: Boolean = false, updatedAfter: Int?, pageAfter: String? = null): Single<Response<RecipientsQuery.Data>> {
        return client.value!!.rxQuery(RecipientsQuery(withTags, Input.fromNullable(updatedAfter), Input.fromNullable(pageAfter)))
            .subscribeOn(Schedulers.io())
            .firstOrError()
    }

    fun queryGroups(updatedAfter: Int?, pageAfter: String? = null): Single<Response<GroupsQuery.Data>> {
        return client.value!!.rxQuery(GroupsQuery(Input.fromNullable(updatedAfter), Input.fromNullable(pageAfter)))
            .subscribeOn(Schedulers.io())
            .firstOrError()
    }
    */

    /*
    fun updateRecipient(id: String, createdAt: Int, recipient: RecipientInput): Single<Response<EditRecipientMutation.Data>> {
        return client.value!!.rxMutate(EditRecipientMutation(
            createdAt, id, recipient
        )).subscribeOn(Schedulers.io())
    }
    */
}