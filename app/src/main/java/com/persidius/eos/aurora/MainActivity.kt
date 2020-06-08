package com.persidius.eos.aurora

import android.annotation.SuppressLint
import com.persidius.eos.aurora.util.Location
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.facebook.stetho.Stetho
import com.google.android.material.navigation.NavigationView
import com.persidius.eos.aurora.auth.AuthManager
import com.persidius.eos.aurora.bluetooth.BTService
import com.persidius.eos.aurora.eos.sync.SyncManager
import com.persidius.eos.aurora.eos.sync.SyncState
import com.persidius.eos.aurora.util.FeatureManager
import com.persidius.eos.aurora.util.Preferences
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class MainActivity : AppCompatActivity() {
    private lateinit var  _location: Location
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var _navController: NavController
    private lateinit var subs: CompositeDisposable;

    // Accessible from fragments
    val navController get() = this._navController
    val location get() = this._location

    inner class DrawerMenu(n: NavigationView) {
        val login: MenuItem = n.menu.findItem(R.id.nav_login)
        val recipientSearch: MenuItem = n.menu.findItem(R.id.nav_searchRecipient)
        val createBin: MenuItem = n.menu.findItem(R.id.nav_cameraScanner)!!
    }

    private lateinit var menu: DrawerMenu

    @SuppressLint("AutoDispose")
    private fun bindHeaderView(navView: NavigationView) {
        val headerView = navView.getHeaderView(0)
        val usernameText = headerView.findViewById<TextView>(R.id.username)
        val syncStatus = headerView.findViewById<TextView>(R.id.syncStatus)
        val syncProgress = headerView.findViewById<TextView>(R.id.syncProgress)

        subs.add(authMgr.session.email
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.value == null) {
                    usernameText.text = "Neautentificat"
                } else {
                    usernameText.text = it.value
                }
            })

        subs.add(
        syncMgr.progress
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { it ->
            syncProgress.text = when(Preferences.smState.value) {
                null,
                SyncState.SYNC_WAIT -> Preferences.smLastSync.value!!
                SyncState.WAIT_VALID_SESSION -> "- - -"
                SyncState.UPDATE_RECIPIENTS,
                SyncState.UPDATE_TAGS,
                SyncState.GROUPS,
                SyncState.RECIPIENTS,
                SyncState.TAGS,
                SyncState.SYNC_RECIPIENTS,
                SyncState.SYNC_GROUPS,
                SyncState.SYNC_TAGS,
                SyncState.DEFINITIONS -> "Progres: ${"%.0f".format(it * 100)}%"
            }
        })
        subs.add(
            Preferences.smState
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { it ->
                    syncStatus.text = when(it) {
                        SyncState.DEFINITIONS -> "Act. Definitii"
                        SyncState.RECIPIENTS, SyncState.SYNC_RECIPIENTS -> "Sinc. Recipienti"
                        SyncState.GROUPS, SyncState.SYNC_GROUPS -> "Sinc. Grupuri"
                        SyncState.TAGS, SyncState.SYNC_TAGS -> "Sinc. Taguri"
                        SyncState.UPDATE_TAGS -> "Act. Taguri"
                        SyncState.UPDATE_RECIPIENTS -> "Act. Recipienti"
                        SyncState.WAIT_VALID_SESSION -> "Asteptare Login"
                        SyncState.SYNC_WAIT -> "Sincronizat"
                        null -> "Null?"
                    }
                    Log.d("STATUS", "${syncStatus.text}")
                }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        location.handleActivityResult(requestCode, resultCode, data)
    }

    @SuppressLint( "CheckResult", "AutoDispose")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        subs = CompositeDisposable()

        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
        }
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        _navController = findNavController(R.id.nav_host_fragment)

        // Passing each menu ID as a set of Ids because each menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_settings, R.id.nav_no_permissions, R.id.nav_login, R.id.nav_searchRecipient), drawerLayout
        )

        setupActionBarWithNavController(_navController, appBarConfiguration)
        navView.setupWithNavController(_navController)
        bindHeaderView(navView)

        menu = DrawerMenu(navView)

        // Enable Login when session is invalid
        subs.add(authMgr.session.isValid.observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                menu.login.isEnabled = !it
            })

        subs.add(featMgr.createBinEnabled.subscribe {
            menu.createBin.isEnabled = it
        })

        subs.add(featMgr.searchBinEnabled.subscribe {
            menu.recipientSearch.isEnabled = it
        })

        _location = Location(this)
    }

    /** Hide keyboard when a field loses focus (tap outside) */
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm: InputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    private var customBackListener: (() -> Unit)? = null

    fun setOnBackListener(listener: () -> Unit) {
        customBackListener = listener
    }

    fun clearOnBackListener() {
        customBackListener = null
    }

    override fun onSupportNavigateUp(): Boolean {
        if (customBackListener != null) {
            customBackListener!!()
            return false
        }
        return _navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    internal val btSvc: BTService
        get() = (application as AuroraApp).btSvc

    internal val authMgr: AuthManager
        get() = (application as AuroraApp).authMgr

    internal val featMgr: FeatureManager
        get() = (application as AuroraApp).featMgr

    internal val syncMgr: SyncManager
        get() = (application as AuroraApp).syncMgr
}
