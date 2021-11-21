package com.persidius.eos.aurora.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.persidius.eos.aurora.core.collection.CollectionsSettings
import com.persidius.eos.aurora.eos.sync.SyncState
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

object Preferences: SharedPreferences.OnSharedPreferenceChangeListener, CollectionsSettings {
  private const val AM_LOCKED = "amLocked"
  private const val AM_ACCESS_TOKEN = "amAccessToken"
  private const val AM_REFRESH_TOKEN = "amRefreshToken"
  private const val AM_USERNAME = "amUsername"
  private const val AM_PASSWORD = "amPassword"

  private const val BT_ENABLED = "btEnabled"
  private const val BT_DEVICE_TYPE = "btDeviceType"
  private const val BT_DEVICE_ID = "btDeviceId"

  private const val SM_STATE = "smState"
  private const val SM_LAST_SYNC = "smLastSync"

  private const val MINIMUM_TIME_BETWEEN_COLLECTIONS = "coMinimumTimeBetweenCollections"
  private const val VEHICLE_LICENSE_PLATE = "coVehicleLicensePlate"

  // Vibrate when a tag reassignment is discovered
  private const val REASSIGN_WARNING = "reassignWarning"

  lateinit var prefs: SharedPreferences

  // AuthorizationManager Token
  lateinit var amLocked: BehaviorSubject<Boolean>
  lateinit var amUsername: BehaviorSubject<String>
  lateinit var amPassword: BehaviorSubject<String>
  lateinit var amAccessToken: BehaviorSubject<String>
  lateinit var amRefreshToken: BehaviorSubject<String>

  lateinit var btEnabled: BehaviorSubject<Boolean>
  lateinit var btDeviceId: BehaviorSubject<String>
  lateinit var btDeviceType: BehaviorSubject<String>

  lateinit var smState: BehaviorSubject<SyncState>
  lateinit var smLastSync: BehaviorSubject<String>

  override lateinit var vehicleLicensePlate: BehaviorSubject<String>
  override lateinit var minimumTimeBetweenCollections: BehaviorSubject<Int>

  lateinit var reassignWarning: BehaviorSubject<Boolean>

  fun init(applicationContext: Context) {
    prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
    initSmPrefs()
    initAmPrefs()
    initBtPrefs()
    initCollectionPrefs()
    initOtherPrefs()

    prefs.registerOnSharedPreferenceChangeListener(this)
  }

  override fun onSharedPreferenceChanged(
    sharedPreferences: SharedPreferences,
    key: String?
  ) {
    when (key) {
      BT_ENABLED -> {
        btEnabled.onNext(sharedPreferences.getBoolean(key, false))
      }
      BT_DEVICE_TYPE -> {
        btDeviceType.onNext(sharedPreferences.getString(key, "")!!)
      }
      BT_DEVICE_ID -> {
        btDeviceId.onNext(sharedPreferences.getString(key, "")!!)
      }
      REASSIGN_WARNING -> {
        reassignWarning.onNext(sharedPreferences.getBoolean(key, true))
        Log.d("Prefs", "ReassignWarning: ${sharedPreferences.getBoolean(key, true)}")
      }
    }
  }

  private fun initCollectionPrefs() {
    minimumTimeBetweenCollections = PrefUtils.setupInt(MINIMUM_TIME_BETWEEN_COLLECTIONS, 500)
    vehicleLicensePlate = PrefUtils.setupString(VEHICLE_LICENSE_PLATE, "NOT-SET")
  }

  private fun initOtherPrefs() {
    reassignWarning = PrefUtils.setupBoolean(REASSIGN_WARNING, true)
  }

  private fun initSmPrefs() {
    smState = setupSmState(SM_STATE, SyncState.WAIT_VALID_SESSION)
    smLastSync = PrefUtils.setupString(SM_LAST_SYNC, "")
  }

  private fun setupSmState(prefName: String, defValue: SyncState): BehaviorSubject<SyncState> {
    val s = try {
      BehaviorSubject.createDefault(SyncState.valueOf(prefs.getString(prefName, defValue.toString()) ?: defValue.toString()))
    } catch (t: Throwable) {
      Log.d("PREFS", "Error loading $prefName: $t")
      BehaviorSubject.createDefault(defValue)
    }

    PrefUtils.subscribeString(s.map { it.toString() }, prefName)

    return s
  }

  private fun initBtPrefs() {
    btEnabled = PrefUtils.setupBoolean(BT_ENABLED, false)
    btDeviceId = PrefUtils.setupString(BT_DEVICE_ID, "")
    btDeviceType = PrefUtils.setupString(BT_DEVICE_TYPE, "")
  }

  private fun initAmPrefs() {
    amLocked = PrefUtils.setupBoolean(AM_LOCKED, false)
    amAccessToken = PrefUtils.setupString(AM_ACCESS_TOKEN, "")
    amRefreshToken = PrefUtils.setupString(AM_REFRESH_TOKEN, "")
    amPassword = PrefUtils.setupString(AM_PASSWORD, "")
    amUsername = PrefUtils.setupString(AM_USERNAME, "")
  }

  private object PrefUtils {
    @SuppressLint("CheckResult")
    fun subscribeString(subject: Observable<String>, prefName: String) {
      subject.subscribe {
        prefs.edit(commit = true) { putString(prefName, it) }
      }
    }

    @SuppressLint("CheckResult")
    fun subscribeInt(subject: BehaviorSubject<Int>, prefName: String) {
      subject.subscribe {
        prefs.edit(commit = true) { putInt(prefName, it) }
      }
    }

    @SuppressLint("CheckResult")
    fun subscribeBoolean(subject: BehaviorSubject<Boolean>, prefName: String) {
      subject.subscribe {
        prefs.edit(commit = true) { putBoolean(prefName, it) }
      }
    }

    fun createString(prefName: String, defValue: String): BehaviorSubject<String> {
      return try {
        BehaviorSubject.createDefault(prefs.getString(prefName, defValue))
      } catch (t: Throwable) {
        Log.d("PREFS", "Error loading $prefName: $t")
        BehaviorSubject.createDefault(defValue)
      }
    }

    fun createBoolean(prefName: String, defValue: Boolean): BehaviorSubject<Boolean> {
      return try {
        BehaviorSubject.createDefault(prefs.getBoolean(prefName, defValue))
      } catch (t: Throwable) {
        Log.d("PREFS", "Error loading $prefName: $t")
        BehaviorSubject.createDefault(defValue)
      }
    }

    fun createInt(prefName: String, defValue: Int): BehaviorSubject<Int> = try {
      BehaviorSubject.createDefault(prefs.getInt(prefName, defValue))
    } catch (t: Throwable) {
      Log.d("PREFS", "Error loading $prefName: $t")
      BehaviorSubject.createDefault(defValue)
    }

    fun setupString(prefName: String, defValue: String): BehaviorSubject<String> {
      val s = createString(prefName, defValue)
      subscribeString(s, prefName)
      return s
    }

    fun setupInt(prefName: String, defValue: Int): BehaviorSubject<Int> {
      val s = createInt(prefName, defValue)
      subscribeInt(s, prefName)
      return s
    }

    fun setupBoolean(prefName: String, defValue: Boolean): BehaviorSubject<Boolean> {
      val s = createBoolean(prefName, defValue)
      subscribeBoolean(s, prefName)
      return s
    }
  }
}
