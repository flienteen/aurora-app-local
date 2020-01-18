package com.persidius.eos.aurora.authorization

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.map
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.auth0.android.jwt.JWT
import com.persidius.eos.aurora.BuildConfig
import com.persidius.eos.aurora.util.Optional
import com.persidius.eos.aurora.util.Preferences
import com.persidius.eos.aurora.util.asLiveData
import com.persidius.eos.aurora.util.asOptional
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import retrofit2.HttpException
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*


// TODO: Shared prefs should be moved into the Preferences object
// TODO: Convert LiveData usage to Observables

/**
 * Shared Preference Keys
 *
 * AMTokenUser - Username used to refresh token
 * AMTokenPassword - Password used to refresh token
 * AMToken - Current token
 * AMLocked - Locked to current account/password, Sign Out is forbidden.
 *
 */

class AuthorizationManager() {

    private companion object {
        val COUNTY_REX = Regex("^c[0-9]+$")
        val UAT_REX = Regex("^u[0-9]+$")
        const val TOKEN_LEEWAY = 300L
    }

    private inner class UserClaim {
        var id: Int = 0
        var name: String = ""
        var email: String = ""
        var enforce2FA: Boolean = false
    }

    data class SessionToken(val jwt: JWT, val email: String, val name: String, val uid: Int, val roles: List<Role>, val countyIdLimit: List<Int>?, val uatIdLimit: List<Int>?) {
        fun hasRole(role: Role): Boolean {
            return (role in roles)
        }

        fun hasRoles(vararg roles: Role): Boolean {
            return roles.map { hasRole(it) }
                .reduce { acc, b -> b && acc }
        }

        fun hasUatAccess(id: Int): Boolean {
            return (uatIdLimit?.indexOf(id) ?: 0) != -1
        }

        fun hasCountyAccess(id: Int): Boolean {
            return (countyIdLimit?.indexOf(id) ?: 0) != -1
        }

        fun printRoles() {
            Log.d("AM", "Session token roles: ${roles.joinToString(",") { it.name }}")
        }

        override fun toString(): String {
            return jwt.toString()
        }
    }

    // Public only properties here.
    inner class Session {
        val email: LiveData<String> = Preferences.amTokenUsername.asLiveData()

        val token: LiveData<JWT?> = map<Optional<JWT>, JWT?>(
            this@AuthorizationManager.token.asLiveData()
        ) { v -> v.value }

        val sessionToken: LiveData<SessionToken?> = map<Optional<SessionToken>, SessionToken?>(
            this@AuthorizationManager.sessionToken.asLiveData()
        ) { v ->
            Log.d("AM", "Set ST Value: $v, thread: ${Thread.currentThread().id}")
            v.value
        }

        val locked: LiveData<Boolean> = Preferences.amLocked.asLiveData()

        val signedIn: LiveData<Boolean> = map<JWT?, Boolean>(token) { !((it?.isExpired(TOKEN_LEEWAY)) ?: true) }
        val loggingIn: LiveData<Boolean> = this@AuthorizationManager.loggingIn

        val error: LiveData<ErrorCode> = this@AuthorizationManager.error
    }

    // token user/pass
    private val token: BehaviorSubject<Optional<JWT>> = BehaviorSubject.createDefault<Optional<JWT>>(null.asOptional())
    val tokenObservable = token as Observable<Optional<JWT>>
    val sessionToken: Observable<Optional<SessionToken>> = token.map { v -> jwtToSessionToken(v) }

    private fun jwtToSessionToken(tkn: Optional<JWT>): Optional<SessionToken> {
        if (!tkn.isPresent()) {
            return null.asOptional()
        } else {
            val roleCodes = tkn.get().getClaim("auz").asList(String::class.java)


            var uatIds: List<Int>? = roleCodes
                .filter { COUNTY_REX.matches(it) }
                .map { it.substring(1).toInt() }

            var countyIds: List<Int>? = roleCodes
                .filter { UAT_REX.matches(it) }
                .map { it.substring(1).toInt() }

            if (uatIds!!.indexOf(0) != -1) uatIds = null
            if (countyIds!!.indexOf(0) != -1) countyIds = null

            val user = tkn.get().getClaim("user").asObject(UserClaim::class.java)

            user ?: return null.asOptional()

            val roles = roleCodes
                .mapNotNull { code -> Role.fromCode(code) }

            Log.d("AM", "ST value set non null")
            return SessionToken(tkn.get(), user.email, user.name, user.id, roles, countyIds, uatIds).asOptional()
        }
    }


    // Whether usename field is locked cand cannot be changed
    private val loggingIn = MutableLiveData<Boolean>(false)
    private val error = MutableLiveData<ErrorCode>()

    enum class ErrorCode() {
        NO_ERROR,

        // Invalid Credentials used.
        LOGIN_FAILED_INVALID_CREDENTIALS,

        // Cannot use a different username than the one provided
        LOGIN_FAILED_WRONG_USER,

        // Login failed du
        LOGIN_FAILED_OTHER_ERROR,
    }

    val session = Session()

    private val userApi: BehaviorSubject<UserAPI>

    init {

        try {
            token.onNext(JWT(Preferences.amToken.value!!).asOptional())
        } catch (e: Exception) {
            token.onNext(null.asOptional())
        }

        token.subscribe { tkn ->
            Preferences.amToken.onNext(if (tkn.isPresent()) tkn.get().toString() else "")
        }

        // UserAPI observable
        Log.d("AM", "${Preferences.eosEnv.value!!}")
        userApi = BehaviorSubject.createDefault(createUserAPI(Preferences.eosEnv.value!!))
        Preferences.eosEnv.subscribe { v -> userApi.onNext(createUserAPI(v)) }

        SessionRefreshHandler(this)

        if (BuildConfig.DEBUG) {
            sessionToken.subscribe {
                if (it.isPresent()) {
                    it.get().printRoles()
                }
            }
        }
    }

    private fun createUserAPI(domain: String): UserAPI {
        return UserAPI.create("https://eos-api.$domain/user/")
    }

    fun isLocked(): Boolean {
        return session.locked.value ?: false
    }

    fun isSignedIn(): Boolean {
        return session.signedIn.value ?: false
    }

    fun login(newUsername: String, newPassword: String) {
//        if (isSignedIn()) {
//            return
//        }

        // don't allow changing usernames if we're locked.
        if (isLocked() && newUsername !== Preferences.amTokenUsername.value) {
            error.value = ErrorCode.LOGIN_FAILED_WRONG_USER
            return
        }

        // Don't allow multiple login attempts at once.
        if (loggingIn.value!!) {
            return
        }

        loggingIn.value = true
        userApi.value!!.Login(UserAPI.Credentials(newUsername, newPassword))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .firstOrError()
            .subscribe(
                { result ->
                    try {
                        val newToken = JWT(result.token)
                        if (!newToken.isExpired(TOKEN_LEEWAY)) {
                            token.onNext(newToken.asOptional())
                            Preferences.amTokenUsername.onNext(newUsername)
                            Preferences.amTokenPassword.onNext(newPassword)
                        }
                    } catch (e: Exception) {
                        Log.d("AM", "Exception @ sign in $e")
                        error.value = ErrorCode.LOGIN_FAILED_OTHER_ERROR
                    }
                    error.value = ErrorCode.NO_ERROR
                    loggingIn.value = false
                },
                { e ->
                    error.value = ErrorCode.LOGIN_FAILED_OTHER_ERROR
                    if (e is HttpException && e.code() == 403) {
                        error.value = ErrorCode.LOGIN_FAILED_INVALID_CREDENTIALS
                    }
                    loggingIn.value = false
                }
            )
    }

    fun logout() {
        // Can't logout when locked
        if (isLocked()) {
            return
        }

        if (!isSignedIn()) {
            return
        }

        // dump all the state
        token.onNext(null.asOptional())
        Preferences.amTokenUsername.onNext("")
        Preferences.amTokenPassword.onNext("")
    }

    /**
     * Lock the manager, not allowing accounts to be swapped
     */
    fun lock() {
        if (isSignedIn()) {
            Preferences.amLocked.onNext(true)
        }
    }

    /**
     * Unlock the authManager, allowing accounts to be switched & logouts to be performed.
     */
    fun unlock() {
        if (!isSignedIn()) {
            Preferences.amLocked.onNext(false)
        }
    }

    fun autoLogin() {
        val email: LiveData<String> = Preferences.amTokenUsername.asLiveData()
        val pass: LiveData<String> = Preferences.amTokenPassword.asLiveData()
        login(email.value!!, pass.value!!)
    }

}