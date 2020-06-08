package com.persidius.eos.aurora.ui.settings

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.persidius.eos.aurora.BuildConfig
import com.persidius.eos.aurora.MainActivity
import com.persidius.eos.aurora.R
import com.persidius.eos.aurora.util.asLiveData
import com.persidius.eos.aurora.bluetooth.BTService
import io.reactivex.android.schedulers.AndroidSchedulers

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<Preference>("version")?.summaryProvider = Preference.SummaryProvider<Preference> {
            "${BuildConfig.VERSION_NAME} ${if (BuildConfig.DEBUG) "debug" else ""}"
        }

        val mainActivity = requireActivity() as MainActivity

        val devIdPref = findPreference<ListPreference>("btDeviceId")
        devIdPref?.entries = mainActivity.btSvc.getDeviceList().map { it.name }.toTypedArray()
        devIdPref?.entryValues = mainActivity.btSvc.getDeviceList().map { it.id }.toTypedArray()
        devIdPref?.summaryProvider = Preference.SummaryProvider<ListPreference> {
            it.value ?: "Selectare dispozitiv bluetooth"
        }

        val devTypePref = findPreference<ListPreference>("btDeviceType")
        devTypePref?.entries = mainActivity.btSvc.getDeviceTypeList().toTypedArray()
         devTypePref?.entryValues = mainActivity.btSvc.getDeviceTypeList().toTypedArray()
        devTypePref?.summaryProvider = Preference.SummaryProvider<ListPreference> {
            it.value ?: "Selectare tip dispozitiv"
        }

        val btStateObserver = Observer<BTService.State?> { newState ->
            val name = when (newState) {
                BTService.State.BT_DISABLED -> "Bluetooth Dezactivat"
                BTService.State.DISABLED -> "Dezactivat"
                BTService.State.ENABLED -> "Neconfigurat"
                BTService.State.CONNECTING -> "Conectare..."
                BTService.State.CONNECTED -> "Conectat"
                else -> "Necunoscut"
            }
            findPreference<PreferenceCategory>("btStatus")?.summary = name
        }

        val btDataObserver = Observer<String?> { newData ->
            findPreference<Preference>("btLastTag")?.summary = newData ?: "Nu există citiri"
        }
        mainActivity.btSvc.tagLiveData.observe(this, btDataObserver)
        mainActivity.btSvc.getState().asLiveData().observe(this, btStateObserver)

        // "Logout" is only enabled if signedIn && not locked
        // switch server is only enabled if !signedOut && not locked
        val updateAMSettings = {
            Log.d("SETTINGS", "Update AM Settings")
            val locked = mainActivity.authMgr.isLocked.value ?: false
            val signedIn = mainActivity.authMgr.session.isValid.blockingFirst()

            Log.d("SETTINGS", "$locked, $signedIn")

            if (!locked) {
                // if not locked, enable eosServer
                findPreference<Preference>("logout")?.isEnabled = signedIn
            } else {
                // if locked, we can't change anything regardless of sign-in status
                findPreference<Preference>("logout")?.isEnabled = false
            }
        }
        mainActivity.authMgr.session.jwt.observeOn(AndroidSchedulers.mainThread()).subscribe { updateAMSettings() }
        mainActivity.authMgr.isLocked.observeOn(AndroidSchedulers.mainThread()).subscribe { updateAMSettings() }

        findPreference<Preference>("logout")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            mainActivity.navController.navigate(R.id.nav_settings, null, NavOptions.Builder().setPopUpTo(R.id.nav_settings, true).build())
            mainActivity.authMgr.logout()
            true
        }

        updateAMSettings()
    }
}
