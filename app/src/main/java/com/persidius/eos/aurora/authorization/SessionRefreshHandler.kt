package com.persidius.eos.aurora.authorization

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.Observer
import com.persidius.eos.aurora.BuildConfig
import com.persidius.eos.aurora.MainActivity
import com.persidius.eos.aurora.R
import com.persidius.eos.aurora.eos.SyncManager
import com.persidius.eos.aurora.util.Optional
import com.persidius.eos.aurora.util.asOptional
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.max

/** Periodically check and refresh the token. In production 6 hours before the token expires, in debug every minute  */
class SessionRefreshHandler(private val activity: MainActivity) {

    private val updateOffset = 360L  // in minutes
    private val am = activity.am
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private val updater = Runnable { scheduleUpdate(true) }

    init {
        am.session.tokenValid.observe(activity, Observer { tkn ->
            if (tkn != null && am.noError()) {
                if (isRunning) {
                    Log.i("SessionRefreshHandler", "Logging in success")
                }
                start()
            } else {
                stop()
                am.forceLogout()
                activity.navController.navigate(R.id.nav_login)
            }
        })

        am.session.error.observe(activity, Observer {
            if (it == AuthorizationManager.ErrorCode.LOGIN_FAILED_INVALID_CREDENTIALS) {
                stop()
                am.forceLogout()
                Log.i("SessionRefreshHandler", "Invalid credentials => logging out")
            }
        })
    }

    @SuppressLint("CheckResult")
    private fun scheduleUpdate(doLogin: Boolean) {
        getTokenExpirationInMinutes().observeOn(AndroidSchedulers.mainThread()).subscribe { expiration ->
            if (expiration.isPresent()) {
                val offset = if (BuildConfig.DEBUG) expiration.get() else updateOffset
                if (doLogin && expiration.get() <= offset) {
                    Log.i("SessionRefreshHandler", "Attempting relogin")
                    am.autoLogin()
                }
                val nextUpdate = max(expiration.get() - offset, 1)
                handler.postDelayed(updater, nextUpdate * 60000)
                Log.i("SessionRefreshHandler", "Token expires in ${expiration.get()} minutes. Next update in $nextUpdate minutes.")
            } else {
                stop()
            }
        }
    }

    private fun getTokenExpirationInMinutes(): Observable<Optional<Long>> {
        return am.session.tokenValidObs.observeOn(Schedulers.computation()).take(1).map<Optional<Long>> { tkn ->
            if (tkn.isPresent()) {
                val exp = tkn.get()?.jwt?.expiresAt
                val expiresAt = LocalDateTime.from(exp?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime())
                val todayTime = LocalDateTime.now()
                return@map Duration.between(todayTime, expiresAt).toMinutes().asOptional()
            }
            return@map Optional<Long>(null)
        }
    }

    private fun start() {
        if (isRunning) {
            return
        }
        isRunning = true
        Log.i("SessionRefreshHandler", "Started")
        scheduleUpdate(false)
    }

    private fun stop() {
        if (!isRunning) {
            return
        }
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        Log.i("SessionRefreshHandler", "Stopped")
    }

    fun isRunning(): Boolean {
        return isRunning
    }
}