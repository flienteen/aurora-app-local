package com.persidius.eos.aurora.authorization

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.map
import com.auth0.android.jwt.JWT
import com.persidius.eos.aurora.BuildConfig
import com.persidius.eos.aurora.util.Optional
import com.persidius.eos.aurora.util.Preferences
import com.persidius.eos.aurora.util.asOptional
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import retrofit2.HttpException


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

class AuthorizationManager {

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
            Log.d("AM", "Session token roles: ${roles.map { it.name }.joinToString(",")}")
        }

        override fun toString(): String {
            return jwt.toString()
        }
    }

    // Public only properties here.
    inner class Session {
        val email: LiveData<String> = this@AuthorizationManager.username

        val token: LiveData<JWT?> = map<Optional<JWT>, JWT?>(
            LiveDataReactiveStreams.fromPublisher(this@AuthorizationManager.token.toFlowable(BackpressureStrategy.LATEST))
        ) { v -> v.value }

        val sessionToken: LiveData<SessionToken?> = map<Optional<SessionToken>, SessionToken?> (
            LiveDataReactiveStreams.fromPublisher(this@AuthorizationManager.sessionToken.toFlowable(BackpressureStrategy.LATEST))
        ) { v -> v.value }

        val locked: LiveData<Boolean> = this@AuthorizationManager.locked
        val signedIn: LiveData<Boolean> = map<JWT?, Boolean>(token) { !((it?.isExpired(TOKEN_LEEWAY)) ?: true) }
        val loggingIn: LiveData<Boolean> = this@AuthorizationManager.loggingIn

        val error: LiveData<ErrorCode> = this@AuthorizationManager.error
    }


    // token user/pass
    private val username = MutableLiveData<String>(Preferences.prefs.getString("AMTokenUsername", ""))
    private val password = MutableLiveData<String>(Preferences.prefs.getString("AMTokenPassword", ""))
    private val token: BehaviorSubject<Optional<JWT>> = BehaviorSubject.create()
    val tokenObservable = token as Observable<Optional<JWT>>
    private val sessionToken = token.map { v -> jwtToSessionToken(v) }

    private fun jwtToSessionToken(tkn: Optional<JWT>): Optional<SessionToken> {
        if(!tkn.isPresent()) {
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
            if (countyIds!!.indexOf(0) !== -1) countyIds = null

            val user = tkn.get().getClaim("user").asObject(UserClaim::class.java)

            user ?: return null.asOptional()

            val roles = roleCodes
                .mapNotNull { code -> Role.fromCode(code) }

            return SessionToken(tkn.get(), user.email, user.name, user.id, roles, countyIds, uatIds).asOptional()
        }
    }


    // Whether usename field is locked cand cannot be changed
    private val locked = MutableLiveData<Boolean>(Preferences.prefs.getBoolean("AMLocked", false))
    private val loggingIn = MutableLiveData<Boolean>(false)
    private val error = MutableLiveData<ErrorCode>()

    enum class ErrorCode(v: Int) {
        NO_ERROR(0),

        // Invalid Credentials used.
        LOGIN_FAILED_INVALID_CREDENTIALS(1),

        // Cannot use a different username than the one provided
        LOGIN_FAILED_WRONG_USER(2),

        // Login failed du
        LOGIN_FAILED_OTHER_ERROR(3),
    }

    val session = Session()

    private val userApi: BehaviorSubject<UserAPI>

    init {

        try {
            token.onNext(JWT(Preferences.amToken.value!!).asOptional())
        } catch(e: Exception) {
            token.onNext(null.asOptional())
        }

        token.subscribe { tkn ->
            Preferences.amToken.onNext(if(tkn.isPresent()) tkn.get().toString() else "") }

        // Auto-save these properties
        username.observeForever {Preferences.prefs.edit().putString("AMTokenUsername", it).apply()}
        password.observeForever {Preferences.prefs.edit().putString("AMTokenPassword", it).apply()}
        locked.observeForever {Preferences.prefs.edit().putBoolean("AMLocked", it).apply()}

        // UserAPI observable
        Log.d("AM", "${Preferences.eosEnv.value!!}")
        userApi = BehaviorSubject.createDefault(createUserAPI(Preferences.eosEnv.value!!))
        Preferences.eosEnv.subscribe { v -> userApi.onNext(createUserAPI(v)) }

        if(BuildConfig.DEBUG) {
            sessionToken.subscribe{
                if(it.isPresent()) {
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
        if(isSignedIn()) {
            return
        }

        // don't allow changing usernames if we're locked.
        if(isLocked() && newUsername !== username.value) {
            error.value = ErrorCode.LOGIN_FAILED_WRONG_USER
            return
        }

        // Don't allow multiple login attempts at once.
        if(loggingIn.value!!) {
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
                    if(!newToken.isExpired(TOKEN_LEEWAY)) {
                        token.onNext(newToken.asOptional())
                        username.value = newUsername
                        password.value = newPassword
                    }
                } catch(e: Exception) {
                    Log.d("AM", "Exception @ sign in" + e.toString())
                    error.value = ErrorCode.LOGIN_FAILED_OTHER_ERROR
                }
                error.value = ErrorCode.NO_ERROR
                loggingIn.value = false
            },
            {e ->
                error.value = ErrorCode.LOGIN_FAILED_OTHER_ERROR
                if(e is HttpException && e.code() === 403) {
                        error.value = ErrorCode.LOGIN_FAILED_INVALID_CREDENTIALS
                }
                loggingIn.value = false
            }
        )
    }

    fun logout() {
        // Can't logout when locked
        if(isLocked()) {
            return
        }

        if(!isSignedIn()) {
            return
        }

        // dump all the state
        token.onNext(null.asOptional())
        username.value = ""
        password.value = ""
    }

    /**
     * Lock the manager, not allowing accounts to be swapped
     */
    fun lock() {
        if(isSignedIn()) {
            locked.postValue(true)
        }
    }

    /**
     * Unlock the authManager, allowing accounts to be switched & logouts to be performed.
     */
    fun unlock() {
        if(!isSignedIn()) {
            locked.postValue(false)
        }
    }
}