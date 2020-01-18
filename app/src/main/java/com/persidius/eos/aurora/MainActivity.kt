package com.persidius.eos.aurora

import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.location.*
import com.google.android.material.navigation.NavigationView
import com.persidius.eos.aurora.authorization.AuthorizationManager
import com.persidius.eos.aurora.authorization.Role
import com.persidius.eos.aurora.authorization.SessionRefreshHandler
import com.persidius.eos.aurora.eos.SyncManager
import com.persidius.eos.aurora.eos.SyncState
import com.persidius.eos.aurora.rfidService.RFIDService
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var _navController: NavController

    private var _lat: Double = 0.0
    private var _lng: Double = 0.0

    val lat: Double get() = _lat
    val lng: Double get() = _lng


    // Accessible from fragments
    val navController get() = this._navController

    inner class DrawerMenu(n: NavigationView) {
        val taskSearch = n.menu.findItem(R.id.nav_searchTask)
        val recipientSearch = n.menu.findItem(R.id.nav_searchRecipient)
        val userSearch = n.menu.findItem(R.id.nav_searchUser)
        val groupSearch = n.menu.findItem(R.id.nav_searchGroup)

        val createEconomicAgent = n.menu.findItem(R.id.nav_createEconomicAgentFlow)
        val createTask = n.menu.findItem(R.id.nav_createTaskFlow)
        val createBin = n.menu.findItem(R.id.nav_cameraScanner)
    }

    private lateinit var menu: DrawerMenu

    private fun bindHeaderView(navView: NavigationView) {
        val headerView = navView.getHeaderView(0)
        val usernameText = headerView.findViewById<TextView>(R.id.username)
        val syncStatus = headerView.findViewById<TextView>(R.id.syncStatus)
        val syncProgress = headerView.findViewById<TextView>(R.id.syncProgress)
        val lastSyncTime = headerView.findViewById<TextView>(R.id.lastSyncTime)

        // username is either <USERNAME> if logged in, or (Neautentificat) if no AM token
        am.session.sessionToken.observe(this, Observer { session ->
            if (session == null) {
                usernameText.text = "Neautentificat"
            } else {
                usernameText.text = session.name
            }
        })

        SyncManager.LiveData.lastSync.observe(this, Observer { newTime ->
            if (newTime != null) {
                if (newTime == 0L) {
                    lastSyncTime.text = "Ultima sincronizare: Niciodată"
                } else {
                    val date = Date(newTime * 1000L)
                    val df = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                    lastSyncTime.text = "Ultima sincronizare: ${df.format(date)}"
                }
            }
        })

        SyncManager.LiveData.progress.observe(this, Observer { newProgress ->
            if (newProgress != null) {
                syncProgress.text = "Progres: ${newProgress}"
            }
        })

        SyncManager.LiveData.state.observe(this, Observer { newState ->
            if (newState != null) {
                fun progressVisible() {
                    syncProgress.visibility = View.VISIBLE
                }

                fun progressHidden() {
                    syncProgress.visibility = View.INVISIBLE
                }

                fun syncTimeVisible() {
                    lastSyncTime.visibility = View.VISIBLE
                }

                fun syncTimeHidden() {
                    lastSyncTime.visibility = View.INVISIBLE
                }

                // Always show last sync status (excep if INIT/lastSync = 0 -> NEVER)
                val statusText = when (newState) {
                    SyncState.INIT -> {
                        progressHidden()
                        syncTimeVisible()
                        "Inițializare"
                    }

                    SyncState.SYNCHRONIZED -> {
                        progressHidden()
                        syncTimeVisible()
                        "Sincronizat"
                    }

                    SyncState.DL_COUNTY -> {
                        progressVisible()
                        syncTimeVisible()
                        "Descarcare definitii județe"
                    }

                    SyncState.DL_UAT -> {
                        progressVisible()
                        syncTimeVisible()
                        "Descărcare definiții UAT"
                    }

                    SyncState.DL_LOC -> {
                        progressVisible()
                        syncTimeVisible()
                        "Descărcare definiții localități"
                    }

                    SyncState.DL_ARTERY -> {
                        progressVisible()
                        syncTimeVisible()
                        "Descărcare definiții străzi"
                    }

                    SyncState.DL_LABELS -> {
                        progressVisible()
                        syncTimeVisible()
                        "Descărcare definiții etichete"
                    }

                    SyncState.WAIT_INTERNET_BACKOFF -> {
                        progressHidden()
                        syncTimeVisible()
                        "Așteptare internet"
                    }

                    SyncState.SYNC_RECIPIENTS_WAIT_INTERNET -> {
                        progressHidden()
                        syncTimeVisible()
                        "Așteptare internet (recipienți)"
                    }

                    SyncState.SYNC_GROUPS_WAIT_INTERNET -> {
                        progressHidden()
                        syncTimeVisible()
                        "Așteptare internet (grupuri)"
                    }

                    SyncState.SYNC_USERS_WAIT_INTERNET -> {
                        progressHidden()
                        syncTimeVisible()
                        "Așteptare internet (utilizatori)"
                    }

                    SyncState.SYNC_TASKS_WAIT_INTERNET -> {
                        progressHidden()
                        syncTimeVisible()
                        "Așteptare internet (taskuri)"
                    }

                    SyncState.OUT_OF_SYNC -> {
                        progressHidden()
                        syncTimeVisible()
                        "Desincronizat (așteptare internet)"
                    }

                    SyncState.SYNC_RECIPIENTS -> {
                        progressVisible()
                        syncTimeVisible()
                        "Descărcare recipienți"
                    }

                    SyncState.SYNC_GROUPS -> {
                        progressVisible()
                        syncTimeVisible()
                        "Descărcare grupuri"
                    }

                    SyncState.SYNC_USERS -> {
                        progressVisible()
                        syncTimeVisible()
                        "Descărcare utilizatori"
                    }

                    SyncState.SYNC_TASKS -> {
                        progressVisible()
                        syncTimeVisible()
                        "Sincronizare taskuri"
                    }

                    SyncState.SYNC_PATCHES -> {
                        progressVisible()
                        syncTimeVisible()
                        "Actualizare editări"
                    }

                    SyncState.UPDATE_TASKS -> {
                        progressVisible()
                        syncTimeVisible()
                        "Actualizare taskuri"
                    }


                    SyncState.ABORTED -> {
                        progressHidden()
                        syncTimeHidden()
                        "Anulare"
                    }
                }
                syncStatus.text = "Status: $statusText"
            }
        })
    }

    private fun initLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationCallback: LocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult?.locations) {
                    _lat = location.latitude
                    _lng = location.longitude
                }
            }
        }

        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 30 * 1000.toLong()
        locationRequest.fastestInterval = 1000
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        _navController = findNavController(R.id.nav_host_fragment)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(R.id.nav_settings,
            R.id.nav_status, R.id.nav_login, R.id.nav_searchRecipient), drawerLayout)

        setupActionBarWithNavController(_navController, appBarConfiguration)
        navView.setupWithNavController(_navController)
        bindHeaderView(navView)

        menu = DrawerMenu(navView)
        updateMenuItems(am.session.sessionToken.value)
        am.session.sessionToken.observe(this, Observer {
            updateMenuItems(it)
        })
        initLocationServices()
        SessionRefreshHandler(this)
    }

    /** Hide keyboard when a field loses focus (tap outside) */
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    /**
     * Updates menu items enabled/disabled state.
     */
    private fun updateMenuItems(s: AuthorizationManager.SessionToken?) {
        if (s == null) {
            menu.taskSearch.isEnabled = false
            menu.groupSearch.isEnabled = false
            menu.recipientSearch.isEnabled = false
            menu.userSearch.isEnabled = false

            menu.createEconomicAgent.isEnabled = false
            menu.createBin.isEnabled = false
            menu.createTask.isEnabled = false
        } else {
            // TODO: Enable in R3
            menu.taskSearch.isEnabled = s.hasRole(Role.LOGISTICS_VIEW_TASK)
            menu.createTask.isEnabled = false /* s.hasRoles(
                Role.LOGISTICS_CREATE_TASK,
                Role.LOGISTICS_EDIT_TASK
            )*/

            menu.recipientSearch.isEnabled = s.hasRole(Role.LOGISTICS_VIEW_RECIPIENT)
            menu.createBin.isEnabled = s.hasRole(Role.LOGISTICS_EDIT_RECIPIENT)

            // TODO: Enable in R2
            menu.userSearch.isEnabled = false // s.hasRole(Role.LOGISTICS_VIEW_USER)
            menu.groupSearch.isEnabled = false //s.hasRole(Role.LOGISTICS_VIEW_GROUPS)
            menu.createEconomicAgent.isEnabled = false /* s.hasRoles(
                Role.LOGISTICS_ALLOC_USER,
                Role.LOGISTICS_EDIT_USER,
                Role.LOGISTICS_VIEW_RECIPIENT,
                Role.LOGISTICS_VIEW_GROUPS
            ) */
        }
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

    internal val rfidService: RFIDService
        get() = (application as AuroraApp).rfidService

    internal val am: AuthorizationManager
        get() = (application as AuroraApp).authorizationManager
}
