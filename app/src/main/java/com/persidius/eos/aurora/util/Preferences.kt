package com.persidius.eos.aurora.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.auth0.android.jwt.JWT
import io.reactivex.subjects.BehaviorSubject


const val EOS_ENV_PREF = "eosEnv"
const val AM_TOKEN = "amToken"

object Preferences {
    lateinit var prefs: SharedPreferences


    // We need to init this with an applicationContext
    // therefore the init needs to be called on app startup.
    lateinit var eosEnv: BehaviorSubject<String>

    // AuthorizationManager Token
    // tapped w/ onNext such that settings are written back to the preferences.
    // there is no preferenceChangeListener for this.
    lateinit var amToken: BehaviorSubject<String>

    fun init(applicationContext: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)

        eosEnv = BehaviorSubject.createDefault<String>(prefs.getString(EOS_ENV_PREF, "persidius.com"))

        amToken = BehaviorSubject.createDefault(prefs.getString(AM_TOKEN, ""))
        amToken.subscribe {
            prefs.edit(commit = true) { putString(AM_TOKEN, it) } }
    }

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener {
            sharedPrefs, key ->
        // what we care about most here is the 'eosEnv' variable.
        when(key) {
            EOS_ENV_PREF -> {
                val newValue = sharedPrefs.getString(key, "persidius.com")
                if(newValue != null) eosEnv.onNext(newValue)
            }
        }
    }
}