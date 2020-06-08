package com.persidius.eos.aurora.authorization

import com.persidius.eos.aurora.BuildConfig
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import javax.annotation.Nullable

interface TokenApi {
    data class TokenRequest(
        var grant_type: String,
        var username: String? = null,
        var password: String? = null,
        var scope: String? = null,
        var refresh_token: String? = null,
        var client_id: String,
        var client_secret: String
    )

    data class TokenResponse (
        var access_token: String,
        var token_type: String,
        var expires_in: String,
        var scope: String,
        var id_token: String,
        var refresh_token: String
    )

    @POST("token")
    @Headers("Content-Type: application/json")
    fun token(@Body() request: TokenRequest): Single<TokenResponse>

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
