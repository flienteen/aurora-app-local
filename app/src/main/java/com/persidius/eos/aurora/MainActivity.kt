package com.persidius.eos.aurora

import android.os.Bundle
import android.util.Log
import android.view.Menu
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
import com.google.android.material.navigation.NavigationView
import com.persidius.eos.aurora.authorization.AuthorizationManager
import com.persidius.eos.aurora.authorization.Role
import com.persidius.eos.aurora.rfidService.RFIDService

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

    private lateinit var navController: NavController;

    inner class DrawerMenu(n: NavigationView) {
        val taskSearch = n.menu.findItem(R.id.nav_searchTask)
        val recipientSearch = n.menu.findItem(R.id.nav_searchRecipient)
        val userSearch = n.menu.findItem(R.id.nav_searchUser)
        val groupSearch = n.menu.findItem(R.id.nav_searchGroup)

        val createEconomicAgent = n.menu.findItem(R.id.nav_createEconomicAgentFlow)
        val createTask = n.menu.findItem(R.id.nav_createTaskFlow)
        val createBin = n.menu.findItem(R.id.nav_createRecipient)
    }

    private lateinit var menu: DrawerMenu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment)


        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
         appBarConfiguration = AppBarConfiguration(setOf(R.id.nav_settings,
              R.id.nav_status, R.id.nav_login), drawerLayout)

        menu = DrawerMenu(navView)

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


        updateMenuItems(am.session.sessionToken.value)
        am.session.sessionToken.observe(this, Observer<AuthorizationManager.SessionToken?> {
            updateMenuItems(it)
        })
    }

    /**
     * Updates menu items enabled/disabled state.
     */
    private fun updateMenuItems(s: AuthorizationManager.SessionToken?) {
        if(s == null) {
            menu.taskSearch.isEnabled = false
            menu.groupSearch.isEnabled = false
            menu.recipientSearch.isEnabled = false
            menu.userSearch.isEnabled = false

            menu.createEconomicAgent.isEnabled = false
            menu.createBin.isEnabled = false
            menu.createTask.isEnabled = false
        } else {
            menu.taskSearch.isEnabled = s.hasRole(Role.LOGISTICS_VIEW_TASK)
            menu.recipientSearch.isEnabled = s.hasRole(Role.LOGISTICS_VIEW_RECIPIENT)
            menu.userSearch.isEnabled = s.hasRole(Role.LOGISTICS_VIEW_USER)
            menu.groupSearch.isEnabled = s.hasRole(Role.LOGISTICS_VIEW_GROUPS)

            menu.createEconomicAgent.isEnabled = s.hasRoles(
                Role.LOGISTICS_ALLOC_USER,
                Role.LOGISTICS_EDIT_USER,
                Role.LOGISTICS_VIEW_RECIPIENT,
                Role.LOGISTICS_VIEW_GROUPS
            )

            menu.createTask.isEnabled = s.hasRoles(
                Role.LOGISTICS_CREATE_TASK,
                Role.LOGISTICS_EDIT_TASK
            )
            menu.createBin.isEnabled = s.hasRole(Role.LOGISTICS_EDIT_RECIPIENT)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    internal val rfidService: RFIDService
        get () = (application as AuroraApp).rfidService

    internal val am: AuthorizationManager
        get () = (application as AuroraApp).authorizationManager
}
