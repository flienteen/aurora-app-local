package com.persidius.eos.aurora.authorization

import com.persidius.eos.aurora.BuildConfig
import io.reactivex.Observable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface UserAPI {
    data class Credentials(
        var email: String,
        var password: String
    )

    data class LoginResponse (
        var token: String
    )

    @POST("login")
    @Headers("Content-Type: application/json")
    fun Login(@Body credentials: Credentials): Observable<LoginResponse>


    companion object {
        fun create(url: String): UserAPI {
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

            return retrofit.build().create(UserAPI::class.java)
        }
    }
}
