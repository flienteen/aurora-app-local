package com.persidius.eos.aurora.authorization

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.map
import androidx.preference.PreferenceManager
import com.auth0.android.jwt.JWT
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import java.lang.Exception


/**
 * Shared Preference Keys
 *
 * AMTokenUser - Username used to refresh token
 * AMTokenPassword - Password used to refresh token
 * AMToken - Current token
 * AMLocked - Locked to current account/password, Sign Out is forbidden.
 *
 */

class AuthorizationManager(applicationContext: Context) {

    private companion object {
        val COUNTY_REX = Regex("^c[0-9]+$")
        val UAT_REX = Regex("^u[0-9]+$")
        val TOKEN_LEEWAY = 300L
    }

    private inner class UserClaim {
        var id: Int = 0
        var name: String = ""
        var email: String = ""
        var enforce2FA: Boolean = false
    }

    data class SessionToken(val email: String, val name: String, val uid: Int, val roles: List<String>, val countyIdLimit: List<Int>?, val uatIdLimit: List<Int>?) {
        fun hasRole(role: String): Boolean {
            return (role in roles)
        }

        fun hasUatAccess(id: Int): Boolean {
            return (uatIdLimit?.indexOf(id) ?: 0) != -1
        }

        fun hasCountyAccess(id: Int): Boolean {
            return (countyIdLimit?.indexOf(id) ?: 0) != -1
        }
    }

    // Public only properties here.
    inner class Session {
        val email: LiveData<String> = this@AuthorizationManager.username

        val token: LiveData<JWT?> = this@AuthorizationManager.token
        val sessionToken: LiveData<SessionToken?> = map<JWT?, SessionToken>(token) {
            if(it == null) null else {
                val roles = it.getClaim("auz").asList(String::class.java)


                var uatIds: List<Int>? = roles
                    .filter { COUNTY_REX.matches(it) }
                    .map { it.substring(1).toInt() }

                var countyIds: List<Int>? = roles
                    .filter { UAT_REX.matches(it) }
                    .map { it.substring(1).toInt() }

                if(uatIds!!.indexOf(0) != -1) uatIds = null
                if(countyIds!!.indexOf(0) !== -1) countyIds = null

                val user = it.getClaim("user").asObject(UserClaim::class.java)

                Log.d("AM", "${user?.toString() ?: null}")
                user ?: return@map null

                SessionToken(user.email, user.name, user.id, roles, countyIds, uatIds)
            }
        }

        val locked: LiveData<Boolean> = this@AuthorizationManager.locked
        val signedIn: LiveData<Boolean> = this@AuthorizationManager.signedIn
        val loggingIn: LiveData<Boolean> = this@AuthorizationManager.loggingIn

        val error: LiveData<ErrorCode> = this@AuthorizationManager.error
    }



    val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)

    // token user/pass
    private val username = MutableLiveData<String>(prefs.getString("AMTokenUsername", ""))
    private val password = MutableLiveData<String>(prefs.getString("AMTokenPassword", ""))
    private val token = MutableLiveData<JWT>(null)

    // Whether usename field is locked cand cannot be changed
    private val locked = MutableLiveData<Boolean>(prefs.getBoolean("AMLocked", false))
    private val signedIn: LiveData<Boolean> = map<JWT?, Boolean>(token) { !((it?.isExpired(TOKEN_LEEWAY)) ?: true) }
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

    private lateinit var userAPI: UserAPI

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener {
            sharedPrefs, key ->
        // what we care about most here is the 'eosEnv' variable.
        when(key) {
            "eosEnv" -> {
                val newDomain = sharedPrefs.getString(key, "")
                if(newDomain !== "") {
                    if(!isLocked()) {
                        createUserApi(newDomain!!)
                        login(username.value!!, password.value!!)
                    }
                }
            }
        }
    }

    init {
        try {
            token.value = JWT(prefs.getString("AMToken", "")!!)
        } catch(e: Exception) {
            token.value = null
        }

        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)

        // Auto-save these properties
        username.observeForever {prefs.edit().putString("AMTokenUsername", it).commit()}
        password.observeForever {prefs.edit().putString("AMTokenPassword", it).commit()}
        locked.observeForever {prefs.edit().putBoolean("AMLocked", it).commit()}
        token.observeForever{ prefs.edit().putString("AMToken", it?.toString() ?: "").commit() }

        createUserApi(prefs.getString("eosEnv", "persidius.com") ?: "persidius.com")
    }

    private fun createUserApi(domain: String) {
        userAPI = UserAPI.create("https://eos-api.$domain/user/")
    }


    fun isLocked(): Boolean {
        return locked.value ?: false
    }

    fun isSignedIn(): Boolean {
        return signedIn.value ?: false
    }

    private var loginDisposable: Disposable? = null
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
        loginDisposable?.dispose()
        loginDisposable = userAPI.Login(UserAPI.Credentials(newUsername, newPassword))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
            { result ->
                try {
                    val newToken = JWT(result.token)
                    if(!newToken.isExpired(TOKEN_LEEWAY)) {
                        token.value = newToken
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
        token.value = null
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