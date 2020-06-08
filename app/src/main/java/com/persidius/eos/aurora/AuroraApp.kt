package com.persidius.eos.aurora

import android.app.Application
import com.google.firebase.FirebaseApp
import com.persidius.eos.aurora.auth.AuthManager
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.util.Preferences
import com.persidius.eos.aurora.bluetooth.BTService
import com.persidius.eos.aurora.eos.sync.SyncManager
import com.persidius.eos.aurora.util.FeatureManager
import io.sentry.Sentry
import io.sentry.android.AndroidSentryClientFactory

class AuroraApp : Application() {
    internal lateinit var authMgr: AuthManager
    internal lateinit var btSvc: BTService
    internal lateinit var featMgr: FeatureManager
    internal lateinit var syncMgr: SyncManager

    override fun onCreate() {
        super.onCreate()
        Sentry.init(BuildConfig.SENTRY_DSN, AndroidSentryClientFactory(this))
        FirebaseApp.initializeApp(this)
        Preferences.init(applicationContext)
        Database.init(applicationContext)

        authMgr = AuthManager()
        btSvc = BTService(this)
        featMgr = FeatureManager(authMgr)
        syncMgr = SyncManager(authMgr, featMgr)
    }
}