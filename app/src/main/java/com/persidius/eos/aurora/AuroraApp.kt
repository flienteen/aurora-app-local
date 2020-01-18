package com.persidius.eos.aurora

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.persidius.eos.aurora.authorization.AuthorizationManager
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.eos.Eos
import com.persidius.eos.aurora.eos.SyncManager
import com.persidius.eos.aurora.eos.tasks.DownloadCounties
import com.persidius.eos.aurora.eos.tasks.DownloadUats
import com.persidius.eos.aurora.rfidService.RFIDService
import com.persidius.eos.aurora.util.Preferences
import io.sentry.Sentry
import io.sentry.android.AndroidSentryClientFactory

class AuroraApp : Application() {
    internal lateinit var rfidService: RFIDService
    internal lateinit var authorizationManager: AuthorizationManager

    override fun onCreate() {
        super.onCreate()
        Sentry.init(BuildConfig.SENTRY_DSN, AndroidSentryClientFactory(this))
        FirebaseApp.initializeApp(this)

        Preferences.init(applicationContext)

        Database.init(applicationContext)

        authorizationManager = AuthorizationManager()

        rfidService = RFIDService(applicationContext)

        Eos.init(authorizationManager)

        SyncManager.init(authorizationManager, applicationContext)

        // attempt to download counties.
        /* DownloadUats.execute(listOf(6,32)).subscribe { progress ->
            Log.d("DL", "Uats $progress%")
        } */
    }
}