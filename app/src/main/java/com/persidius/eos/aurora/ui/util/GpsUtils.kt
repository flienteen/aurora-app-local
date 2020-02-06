package com.persidius.eos.aurora.ui.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.persidius.eos.aurora.BuildConfig
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit


class GpsUtils(private val activity: Activity) {

    private val gpsRequest = 1001
    private val tag = "GpsUtils"
    private val settingsClient = LocationServices.getSettingsClient(activity)
    private val locationSettingsRequest: LocationSettingsRequest
    private val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val locationRequest = LocationRequest.create()

    var isGpsOn = false

    init {
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 10 * 1000.toLong()
        locationRequest.fastestInterval = 2 * 1000.toLong()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        locationSettingsRequest = builder.build()
        builder.setAlwaysShow(true)
        initInterval()
    }

    @SuppressLint("CheckResult")
    private fun initInterval() {
        val interval = Observable.interval(0L, if (BuildConfig.DEBUG) 60L else 300L, TimeUnit.SECONDS).subscribeOn(Schedulers.io())
        interval.subscribe {
            checkPermissions()
            turnGPSOn()
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == gpsRequest) {
                isGpsOn = true // flag maintain before get location
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            if (requestCode == gpsRequest) {
                turnGPSOn()
            }
        }
    }

    private fun turnGPSOn() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            isGpsOn = true
        } else {
            settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(activity) {
                    //  GPS is already enabled, callback GPS status through listener
                    isGpsOn = true
                }
                .addOnFailureListener(activity) { e ->
                    when ((e as ApiException).statusCode) {
                        // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                            val rae = e as ResolvableApiException
                            rae.startResolutionForResult(activity, gpsRequest)
                        } catch (sie: SendIntentException) {
                            Log.i(tag, "PendingIntent unable to execute request.")
                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            val errorMessage = "Dezactiveaza modul avion daca este pornit!"
                            Log.e(tag, errorMessage)
                            Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Log.d(tag, "${Manifest.permission.ACCESS_COARSE_LOCATION} is already on")
            } else {
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            }
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.d(tag, "${Manifest.permission.ACCESS_FINE_LOCATION} is already on")
            } else {
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            }
        }
    }
}