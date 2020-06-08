package com.persidius.eos.aurora.auth

import android.util.Log
import com.auth0.android.jwt.JWT
import com.google.android.material.appbar.AppBarLayout
import com.google.gson.Gson
import com.persidius.eos.aurora.BuildConfig
import com.persidius.eos.aurora.util.Optional
import com.persidius.eos.aurora.util.Preferences
import com.persidius.eos.aurora.util.asOptional
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import retrofit2.HttpException
import java.io.IOException
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class AuthManager {
    private val tag = "AM"
    private var tokenApi = TokenApi.create(BuildConfig.IDP_URL)
    private val accessToken: Observable<Optional<JWT>> = Preferences.amAccessToken.map { try { JWT(it) } catch(t: Throwable) { null }.asOptional() }
    private val refreshToken: Observable<Optional<JWT>> = Preferences.amRefreshToken.map { try { JWT(it) } catch(t: Throwable) { null }.asOptional() }
    val isLocked: BehaviorSubject<Boolean>

    companion object  {
        private const val REFRESH_THRESHOLD = -30
    }

    enum class LoginErrorType {
        Network,                // no network is available
        InvalidCredentials,     // credentials are invalid
        Other                   // some other error (usually misconfiguration)
    }

    class LoginError(
        val type: LoginErrorType,
        message: String,
        cause: Throwable
    ): Exception(message,  cause)

    data class EosClaim (
        val roles: List<Role?>,
        val limit_county: String?,
        val limit_uat: String?
    )

    inner class Session {
        val jwt: BehaviorSubject<Optional<JWT>> = BehaviorSubject.createDefault<Optional<JWT>>(null.asOptional())
        val email: BehaviorSubject<Optional<String>> = BehaviorSubject.createDefault<Optional<String>>(null.asOptional())
        val roles: BehaviorSubject<List<Role>> = BehaviorSubject.createDefault(listOf())
        private var disposable: Disposable
        init {
            disposable = accessToken.subscribe {
                try {
                    val token = it.value
                    val rolesArray = token?.claims?.get("eos")?.asObject(EosClaim::class.java)?.roles?.filterNotNull() ?: listOf()
                    val emailString = token?.claims?.get("email")?.asString().asOptional()

                    jwt.onNext(it)
                    roles.onNext(rolesArray)
                    email.onNext(emailString)
                    Log.d(tag, rolesArray.joinToString(","))
                } catch(t: Throwable) {
                    jwt.onNext(null.asOptional())
                    roles.onNext(listOf())
                    email.onNext(null.asOptional())
                    Log.d(tag, "Error parsing JWT", t)
                }
            }
        }

        internal fun dispose() = disposable.dispose()

        fun hasRole(role: Role): Boolean = (role in (roles.value ?: listOf()))
        fun hasRoles(vararg whichRoles: Role): Boolean = whichRoles.map { hasRole(it) }.reduce { acc, b -> b && acc }

        // Whether session is "online" aka the token is valid & can be used.
        val isOnline: Observable<Boolean> = jwt.map { it.value?.isExpired(0)?.not() ?: false }

        // Whether there is any session at all (ie not logged out because of invalid credentials)
        val isValid: Observable<Boolean> = jwt.map { it.value != null }
        override fun toString(): String = jwt.value?.value?.toString() ?: ""
    }

    init {
        val disp = CompositeDisposable()

        if(Preferences.amLocked.value == null) {
            Preferences.amLocked.onNext(false)
        }
        isLocked = BehaviorSubject.createDefault(false)
        disp.add(isLocked.subscribe {
            Preferences.amLocked.onNext(it)
        })

        val loop = Observable.interval(5, TimeUnit.SECONDS)
        disp.add(loop.observeOn(Schedulers.io())
            .subscribe { update() })

        // @Init ensure we update the state
        update()
    }

    val session = Session()


    fun parseTokenError(t: Throwable): LoginError {
        if(t is HttpException) {
            val data = (Gson()).fromJson(t.response().errorBody()?.charStream(), TokenApi.TokenErrorResponse::class.java)
            Log.d(tag, data.toString())
            return if(data.error == TokenApi.TokenError.InvalidGrant) {
                LoginError(LoginErrorType.InvalidCredentials, "Invalid credentials", t)
            } else {
                LoginError(LoginErrorType.Other, data.error_message, t)
            }
        }

        if(t is IOException) {
            return LoginError(LoginErrorType.Network, "Something went wrong with the network", t)
        }

        return LoginError(LoginErrorType.Other, "Unknown error", t)
    }

    /**
     * Returns the result of authenticating the user.
     * Throws an error otherwise.
     */
    fun login(username: String, password: String): Single<TokenApi.TokenResponse> {
        /* todo: check if AM is locked to a user and we're trying to \
             login with another user, if that's the case just refuse to
              run the net request */

        return tokenApi.token(
            grantType = "password",
            clientId = BuildConfig.IDP_CLIENT_ID,
            clientSecret = BuildConfig.IDP_CLIENT_SECRET,
            password = password,
            username = username)
        .subscribeOn(Schedulers.io())
        .doOnSuccess {
            Log.d(tag,"login success")
            Preferences.amRefreshToken.onNext(it.refresh_token)
            Preferences.amAccessToken.onNext(it.access_token)
            Preferences.amPassword.onNext(password)
            Preferences.amUsername.onNext(username)
        }
    }

    // Logs user out.
    fun logout() {
        wipeCredentials()
    }

    fun lock() {
        isLocked.onNext(true)
    }

    fun unlock() {
        isLocked.onNext(false)
    }

    private fun refresh(refreshToken: String): Single<TokenApi.TokenResponse> {
        return tokenApi.token(
            grantType = "refresh_token",
            clientId = BuildConfig.IDP_CLIENT_ID,
            clientSecret = BuildConfig.IDP_CLIENT_SECRET,
            refreshToken = refreshToken
        )
        .subscribeOn(Schedulers.io())
        .doOnSuccess {
            Log.d(tag,"success")
            Preferences.amRefreshToken.onNext(it.refresh_token)
            Preferences.amAccessToken.onNext(it.access_token)
        }
    }


    /**
     * Update logic
     */

    private var pendingUpdateDisposable: Disposable? = null
    fun update(force: Boolean = false) {
        // Skip this loop.
        if(pendingUpdateDisposable != null) {
            if(force) {
                pendingUpdateDisposable?.dispose()
            }
            else {
                Log.d(tag, "Disposable pending, skipping loop.")
                return
            }
        }

        val at = accessToken.blockingFirst().value

        // if AT is valid, we're done.
        if(at != null && !isExpired(at)) { return }

        Log.d(tag, "${at?.expiresAt}, $at")

        val rt = refreshToken.blockingFirst().value
        if(rt != null) {
            if (!isExpired(rt)) {
                pendingUpdateDisposable = refresh(Preferences.amRefreshToken.value!!)
                .doFinally {
                    pendingUpdateDisposable?.dispose()
                    pendingUpdateDisposable = null
                }
                .subscribe({}, {
                    val err = parseTokenError(it)
                    if(err.type == LoginErrorType.InvalidCredentials) {
                        wipeCredentials()
                    }

                    // other errors (such as net errors) we can ignore.
                    Log.e(tag, err.toString(), err.cause)
                })
                return
            }
        }

        if(Preferences.amPassword.value?.length ?: 0 > 0
            && Preferences.amUsername.value?.length ?: 0 > 0) {
            pendingUpdateDisposable = login(Preferences.amUsername.value!! , Preferences.amPassword.value!!)
            .doFinally {
                pendingUpdateDisposable?.dispose()
                pendingUpdateDisposable = null
            }
            .subscribe({}, {
                val err = parseTokenError(it)
                if(err.type == LoginErrorType.InvalidCredentials) {
                    wipeCredentials()
                }

                // other errors (such as net errors) we can ignore.
                Log.e(tag, err.toString(), err.cause)
            })
            return
        }
    }

    private fun wipeCredentials() {
        if(!Preferences.amLocked.value!!) {
            Preferences.amUsername.onNext("")
        }

        Preferences.amPassword.onNext("")
        Preferences.amRefreshToken.onNext("")
        Preferences.amAccessToken.onNext("")
    }

    private fun isExpired(jwt: JWT, threshold: Int = REFRESH_THRESHOLD): Boolean {
        if(jwt.expiresAt == null) {
            return false
        }

        val maxExpiry = Date()
        maxExpiry.time = maxExpiry.time + (threshold * 1000)
        return jwt.expiresAt!!.before(maxExpiry)
    }
}