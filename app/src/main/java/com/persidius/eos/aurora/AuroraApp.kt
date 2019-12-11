package com.persidius.eos.aurora

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.persidius.eos.aurora.authorization.AuthorizationManager
import com.persidius.eos.aurora.rfidService.RFIDService
import io.sentry.Sentry
import io.sentry.android.AndroidSentryClientFactory

class AuroraApp: Application() {
    internal lateinit var rfidService: RFIDService
    internal lateinit var authorizationManager: AuthorizationManager
    override fun onCreate() {
        super.onCreate()
        Sentry.init(BuildConfig.SENTRY_DSN, AndroidSentryClientFactory(this))
        FirebaseApp.initializeApp(this)
        Log.d("barcode",  "firebase init")

        authorizationManager = AuthorizationManager(applicationContext)
        rfidService = RFIDService(applicationContext)
    }
}