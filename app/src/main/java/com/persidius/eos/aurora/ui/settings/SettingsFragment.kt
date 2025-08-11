package com.persidius.eos.aurora.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
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
  private val streamOverrideLabels = arrayOf("Rezidual (Negru)", "Biodegradabil (Maro)", "Plastic și Metal (Galben)", "Hârtie și Carton (Albastru)", "Sticlă (Verde)")
  private val streamOverrideValues = arrayOf("REZ", "BIO", "RPM", "RPC", "RGL")

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.preferences, rootKey)

    findPreference<Preference>("version")?.summaryProvider = Preference.SummaryProvider<Preference> {
      "${BuildConfig.VERSION_NAME} ${if (BuildConfig.DEBUG) "debug" else ""}"
    }

    val mainActivity = requireActivity() as MainActivity

    val devIdPref = findPreference<ListPreference>("btDeviceId")
    devIdPref?.summaryProvider = Preference.SummaryProvider<ListPreference> {
      it.value ?: "Selectare dispozitiv bluetooth"
    }

    requestBluetoothPermission()

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

    val streamOverride = findPreference<ListPreference>("streamOverride")
    streamOverride?.entries = streamOverrideLabels
    streamOverride?.entryValues = streamOverrideValues
    streamOverride?.summaryProvider = Preference.SummaryProvider<ListPreference> {
      if (it.value != null) {
        val labelIndex = streamOverrideValues.indexOf(it.value)
        if (labelIndex != -1) {
          streamOverrideLabels[labelIndex]
        } else {
          it.value
        }
      } else {
        "Atinge pentru a selecta o valoare"
      }
    }

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

  private fun populateBluetoothDeviceList() {
    val mainActivity = requireActivity() as MainActivity
    val devIdPref = findPreference<ListPreference>("btDeviceId")
    devIdPref?.entries = mainActivity.btSvc.getDeviceList().map { it.name }.toTypedArray()
    devIdPref?.entryValues = mainActivity.btSvc.getDeviceList().map { it.id }.toTypedArray()
  }

  private fun requestBluetoothPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      if (ContextCompat.checkSelfPermission(
          requireActivity(),
          Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
      ) {
        populateBluetoothDeviceList()
      } else {
        requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
      }
    } else {
      populateBluetoothDeviceList()
    }
  }

  private val requestPermissionLauncher =
    registerForActivityResult(
      ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
      if (isGranted) {
        populateBluetoothDeviceList()
      }
    }
}
