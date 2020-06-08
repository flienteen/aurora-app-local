package com.persidius.eos.aurora.authorization

import android.util.Log
import com.auth0.android.jwt.JWT
import com.persidius.eos.aurora.BuildConfig
import com.persidius.eos.aurora.util.Preferences
import com.persidius.eos.aurora.util.asOptional
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import retrofit2.HttpException

class AuthManager {
    private var tokenApi = TokenApi.create(BuildConfig.IDP_URL);


    /**
     * Returns the result of authenticating the user.
     */
    fun processLogin(username: String, password: String) {

        /* todo: check if AM is locked to a user and we're trying to \
             login with another user. */

        tokenApi.token(TokenApi.TokenRequest(
            grant_type = "password",
            client_id = BuildConfig.IDP_CLIENT_ID,
            client_secret = BuildConfig.IDP_CLIENT_SECRET,
            password = password,
            username = username
        )).subscribe({ response ->
            Log.d("AM", response.toString())
        }, {
            Log.d("AM", it.toString())
        })
    }
}