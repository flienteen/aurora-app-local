package com.persidius.eos.aurora.auth

import com.persidius.eos.aurora.BuildConfig
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface TokenApi {
    data class TokenResponse (
        var access_token: String,
        var token_type: String,
        var expires_in: String,
        var scope: String,
        var id_token: String,
        var refresh_token: String
    )

    data class TokenErrorResponse (
        var error: String,               // usually 'invalid_grant'
        var error_message: String        // debug error message
    )

    object TokenError {
        val InvalidGrant = "invalid_grant"      // request did not contain valid credentials
        val InvalidClient = "invalid_client"    // request had invalid client id
        val InvalidRequest = "invalid_request"  // request did not contain all necessary fields
        val InvalidScope = "invalid_scope"      // request contained invalid scopes
        val UnsupportedGrantType = "unsupported_grant_type"  // not a valid grant type
    }

    @POST("token")
    @FormUrlEncoded()
    fun token(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String? = null,
        @Field("client_secret") clientSecret: String? = null,
        @Field("username") username: String? = null,
        @Field("password") password: String? = null,
        @Field("scope") scope: String? = null,
        @Field("refresh_token") refreshToken: String? = null
    ): Single<TokenResponse>

    companion object {
        fun create(url: String): TokenApi {
            val retrofit = Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())

            if(BuildConfig.ENABLE_HTTP_LOGGING) {
                val interceptor = HttpLoggingInterceptor()
                interceptor.level = HttpLoggingInterceptor.Level.BODY

                val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
                retrofit.client(client)
            }

            retrofit.baseUrl(url)

            return retrofit.build().create(TokenApi::class.java)
        }
    }
}
