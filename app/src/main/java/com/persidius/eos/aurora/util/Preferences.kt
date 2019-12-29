package com.persidius.eos.aurora.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.auth0.android.jwt.JWT
import com.persidius.eos.aurora.BuildConfig
import com.persidius.eos.aurora.eos.SyncState
import io.reactivex.subjects.BehaviorSubject


const val EOS_ENV_PREF = "eosEnv"

const val AM_TOKEN = "amToken"
const val AM_TOKEN_USERNAME = "amTokenUsername"
const val AM_TOKEN_PASSWORD = "amTokenPassword"
const val AM_LOCKED = "amLocked"

const val SM_SYNC_STATE = "smSyncState"
const val SM_LAST_SYNC = "smLastSync"

const val SM_LAST_RECIPIENT_UPDATE = "smLastRecipientUpdate"
const val SM_LAST_USER_UPDATE = "smLastUserUpdate"
const val SM_LAST_GROUP_UPDATE = "smLastGroupUpdate"
const val SM_LAST_TASK_UPDATE = "smLastTaskUpdate"

object Preferences {
    lateinit var prefs: SharedPreferences


    // We need to init this with an applicationContext
    // therefore the init needs to be called on app startup.
    lateinit var eosEnv: BehaviorSubject<String>

    // AuthorizationManager Token
    // tapped w/ onNext such that settings are written back to the preferences.
    // there is no preferenceChangeListener for this.
    lateinit var amToken: BehaviorSubject<String>
    lateinit var amLocked: BehaviorSubject<Boolean>
    lateinit var amTokenUsername: BehaviorSubject<String>
    lateinit var amTokenPassword: BehaviorSubject<String>

    lateinit var smSyncState: BehaviorSubject<SyncState>
    lateinit var smLastSync: BehaviorSubject<Long>
    lateinit var smLastRecipientUpdate: BehaviorSubject<Long>
    lateinit var smLastUserUpdate: BehaviorSubject<Long>
    lateinit var smLastGroupUpdate: BehaviorSubject<Long>


    val eosEnvDefault = if(BuildConfig.DEBUG) "persidius.dev" else "persidius.com"

    fun init(applicationContext: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        initSmPrefs()
        initOtherPrefs()
        initAmPrefs()

        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener {
            sharedPrefs, key ->
        // what we care about most here is the 'eosEnv' variable.
        when(key) {
            EOS_ENV_PREF -> {
                val newValue = sharedPrefs.getString(key, eosEnvDefault)
                if(newValue != null) eosEnv.onNext(newValue)
            }
        }
    }

    private fun initOtherPrefs() {
        eosEnv = utils.createString(EOS_ENV_PREF, eosEnvDefault)
    }

    private fun initAmPrefs() {
        amToken = utils.setupString(AM_TOKEN, "")
        amLocked = utils.setupBoolean(AM_LOCKED, false)
        amTokenUsername = utils.setupString(AM_TOKEN_USERNAME, "")
        amTokenPassword = utils.setupString(AM_TOKEN_PASSWORD, "")
    }

    private fun initSmPrefs() {
        smSyncState = try {
            BehaviorSubject.createDefault(SyncState.valueOf(prefs.getString(SM_SYNC_STATE, SyncState.INIT.name)!!))
        } catch(t: Throwable) {
            Log.d("PREFS", "Error loading $SM_SYNC_STATE: $t")
            BehaviorSubject.createDefault(SyncState.INIT)
        }

        smSyncState.subscribe { ss: SyncState ->
            prefs.edit(commit = true) { putString(SM_SYNC_STATE, ss.name) }
        }

        smLastSync = utils.setupLong(SM_LAST_SYNC, 0)
        smLastRecipientUpdate = utils.setupLong(SM_LAST_RECIPIENT_UPDATE, 0)
        smLastGroupUpdate = utils.setupLong(SM_LAST_GROUP_UPDATE, 0)
        smLastUserUpdate = utils.setupLong(SM_LAST_USER_UPDATE, 0)
    }

    private object utils {
        fun subscribeString(subject: BehaviorSubject<String>, prefName: String) {
            subject.subscribe {
                prefs.edit(commit = true) { putString (prefName, it) }
            }
        }

        fun subscribeLong(subject: BehaviorSubject<Long>, prefName: String) {
            subject.subscribe {
                prefs.edit(commit = true) { putLong(prefName, it) }
            }
        }

        fun subscribeBoolean(subject: BehaviorSubject<Boolean>, prefName: String) {
            subject.subscribe {
                prefs.edit(commit = true) { putBoolean(prefName, it) }
            }
        }

        fun createString(prefName: String, defValue: String): BehaviorSubject<String> {
            return try {
                BehaviorSubject.createDefault(prefs.getString(prefName, defValue))
            } catch(t: Throwable) {
                Log.d("PREFS", "Error loading $prefName: $t")
                BehaviorSubject.createDefault(defValue)
            }
        }

        fun createBoolean(prefName: String, defValue: Boolean): BehaviorSubject<Boolean> {
            return try {
                BehaviorSubject.createDefault(prefs.getBoolean(prefName, defValue))
            } catch(t: Throwable) {
                Log.d("PREFS", "Error loading $prefName: $t")
                BehaviorSubject.createDefault(defValue)
            }
        }

        fun createLong(prefName: String, defValue: Long): BehaviorSubject<Long> = try {
            BehaviorSubject.createDefault(prefs.getLong(prefName, defValue))
        } catch(t: Throwable) {
            Log.d("PREFS", "Error loading $prefName: $t")
            BehaviorSubject.createDefault(defValue)
        }

        fun setupString(prefName: String, defValue: String): BehaviorSubject<String> {
            val s = createString(prefName, defValue)
            subscribeString(s, prefName)
            return s
        }

        fun setupLong(prefName: String, defValue: Long): BehaviorSubject<Long> {
            val s = createLong(prefName, defValue)
            subscribeLong(s, prefName)
            return s
        }

        fun setupBoolean(prefName: String, defValue: Boolean): BehaviorSubject<Boolean> {
            val s = createBoolean(prefName, defValue)
            subscribeBoolean(s, prefName)
            return s
        }
    }
}
