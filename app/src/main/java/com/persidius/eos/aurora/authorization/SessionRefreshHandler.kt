package com.persidius.eos.aurora.authorization

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Observer
import com.persidius.eos.aurora.MainActivity
import com.persidius.eos.aurora.R
import com.persidius.eos.aurora.util.Optional
import com.persidius.eos.aurora.util.asOptional
import io.reactivex.Observable
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.max

class SessionRefreshHandler(val activity: MainActivity) {

    private val debug = true // for testing, expiration = 1 min
    private val updateOffset = if (debug) 1 else 360L  // in minutes

    private val am = activity.am
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private val updater = Runnable { scheduleUpdate() }

    init {
        am.session.sessionToken.observe(activity, Observer { tkn ->
            if (tkn != null && !tkn.jwt.isExpired(300) && noError()) {
                start()
            } else {
                stop()
                activity.navController.navigate(R.id.nav_login)
            }
        })

        am.session.error.observe(activity, Observer {
            if (it == AuthorizationManager.ErrorCode.LOGIN_FAILED_INVALID_CREDENTIALS) {
                stop()
                activity.navController.navigate(R.id.nav_login)
            }
        })
    }

    @SuppressLint("CheckResult")
    private fun scheduleUpdate() {
        getTokenExpirationInMinutes().subscribe { expiration ->
            if (expiration.isPresent()) {
                if (expiration.get() <= updateOffset) {
                    am.autoLogin()
                }
                val nextUpdate = max(expiration.get() - updateOffset, updateOffset)
                handler.postDelayed(updater, nextUpdate * 60000)
            } else {
                stop()
            }
        }
    }

    private fun getTokenExpirationInMinutes(): Observable<Optional<Long>> {
        return am.sessionToken.take(1).map { tkn ->
            if (tkn.isPresent()) {
                if (debug) {
                    return@map Optional(1L)
                }
                val exp = tkn.get().jwt.expiresAt
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
        scheduleUpdate()
    }

    private fun stop() {
        if (!isRunning) {
            return
        }
        isRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun noError(): Boolean {
        val error = activity.am.session.error.value
        return error == null || error == AuthorizationManager.ErrorCode.NO_ERROR
    }

    fun isRunning(): Boolean {
        return isRunning
    }
}