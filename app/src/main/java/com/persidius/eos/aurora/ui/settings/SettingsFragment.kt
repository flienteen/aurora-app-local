package com.persidius.eos.aurora.ui.settings

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Observer
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.auth0.android.jwt.JWT
import com.persidius.eos.aurora.BuildConfig
import com.persidius.eos.aurora.MainActivity
import com.persidius.eos.aurora.R
import com.persidius.eos.aurora.eos.SyncManager
import com.persidius.eos.aurora.rfidService.RFIDService

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<Preference>("version")?.summaryProvider = Preference.SummaryProvider<Preference> {
            "${BuildConfig.VERSION_NAME} ${if (BuildConfig.DEBUG) "debug" else ""}"
        }

        val mainActivity = activity!! as MainActivity

        val devAddressPref = findPreference<ListPreference>("rfidDeviceAddress")
        devAddressPref?.entries = mainActivity.rfidService.getDeviceList().toTypedArray()
        devAddressPref?.entryValues = mainActivity.rfidService.getDeviceList().toTypedArray()
        devAddressPref?.summaryProvider = Preference.SummaryProvider<ListPreference> {
            it.value?.split("\n")?.get(0) ?: "Selectare dispozitiv bluetooth"
        }

        val devTypePref = findPreference<ListPreference>("rfidDeviceType")
        devTypePref?.entries = mainActivity.rfidService.getDeviceTypeList().toTypedArray()
        devTypePref?.entryValues = mainActivity.rfidService.getDeviceTypeList().toTypedArray()
        devTypePref?.summaryProvider = Preference.SummaryProvider<ListPreference> {
            it.value ?: "Selectare tip dispozitiv"
        }

        findPreference<ListPreference>("eosEnv")?.summaryProvider = Preference.SummaryProvider<ListPreference> {
            it.entry ?: "Selectare server eos"
        }

        val rfidStateObserver = Observer<RFIDService.State> { newState ->
            val name = when (newState) {
                RFIDService.State.DISABLED -> "Dezactivat"
                RFIDService.State.BT_DISABLED -> "Bluetooth Dezactivat"
                RFIDService.State.NOT_CONFIGURED -> "Neconfigurat"
                RFIDService.State.CONNECTING -> "Conectare..."
                RFIDService.State.CONNECTED -> "Conectat"
                else -> "Necunoscut"
            }

            findPreference<PreferenceCategory>("rfidStatus")?.summary = name
        }

        val rfidDataObserver = Observer<String?> { newData ->
            findPreference<Preference>("rfidLastTag")?.summary = newData ?: "Nu există citiri"
        }

        mainActivity.rfidService.liveState.observe(this, rfidStateObserver)
        mainActivity.rfidService.liveData.observe(this, rfidDataObserver)

        // "Logout" is only enabled if signedIn && not locked
        // switch server is only enabled if !signedOut && not locked


        val updateAMSettings = {
            Log.d("SETTINGS", "Update AM Settings")
            val locked = mainActivity.am.session.locked.value ?: false
            val signedIn = mainActivity.am.session.signedIn.value ?: false

            Log.d("SETTINGS", "$locked, $signedIn")

            if (!locked) {
                // if not locked, enable eosServer
                findPreference<ListPreference>("eosEnv")?.isEnabled = !signedIn
                findPreference<Preference>("logout")?.isEnabled = signedIn
            } else {
                // if locked, we can't change anything regardless of sign-in status
                findPreference<ListPreference>("eosEnv")?.isEnabled = false
                findPreference<Preference>("logout")?.isEnabled = false
            }
        }
        val amObserver = Observer<Boolean> { updateAMSettings() }
        mainActivity.am.session.locked.observe(this, amObserver)
        mainActivity.am.session.signedIn.observe(this, amObserver)

        findPreference<Preference>("logout")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            SyncManager.doAbort()
            mainActivity.am.logout()
            true
        }

        updateAMSettings()
    }
}
