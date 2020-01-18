package com.persidius.eos.aurora.authorization

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.work.*
import com.persidius.eos.aurora.util.Optional
import com.persidius.eos.aurora.util.asOptional
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.math.max

class SessionRefreshHandler(val am: AuthorizationManager) {

    private val debug = true // for testing, expiration = 1 min
    private val updateOffset = if (debug) 1 else 360L  // in minutes

    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private val updater = Runnable { scheduleUpdate() }

    init {
        am.tokenObservable.subscribe { tkn ->
            if (tkn.isPresent()) {
                start()
            } else {
                stop()
            }
        }
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

    fun isRunning(): Boolean {
        return isRunning
    }
}