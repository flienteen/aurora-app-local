package com.persidius.eos.aurora.util

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
import com.google.android.gms.location.*
import com.persidius.eos.aurora.BuildConfig
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit


class Location(private val activity: Activity) {

    private val gpsRequest = 1001
    private val tag = "LOC"
    private val settingsClient = LocationServices.getSettingsClient(activity)
    private val locationSettingsRequest: LocationSettingsRequest
    private val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val locationRequest = LocationRequest.create()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var _lat: Double = 0.0
    private var _lng: Double = 0.0

    val lat: Double get() = _lat
    val lng: Double get() = _lng

    private var isGpsOn = false

    init {
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 10 * 1000.toLong()
        locationRequest.fastestInterval = 2 * 1000.toLong()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        locationSettingsRequest = builder.build()
        builder.setAlwaysShow(true)
        initInterval()
        initLocationServices()
    }

    @SuppressLint("CheckResult")
    private fun initInterval() {
        val interval = Observable.interval(0L, 30L, TimeUnit.SECONDS).subscribeOn(Schedulers.io())
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

    private fun initLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
        val locationCallback: LocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    _lat = location.latitude
                    _lng = location.longitude
                }
            }
        }

        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 30 * 1000.toLong()
        locationRequest.fastestInterval = 1000
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } catch(e: SecurityException) {
            Log.d(tag, "Could not start location updates", e)
        }
    }
}