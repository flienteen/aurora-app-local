import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender.SendIntentException
import android.location.LocationManager
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*


class GpsUtils(private val context: Context) {

    companion object {
        const val GPS_REQUEST = 1001
    }

    private val mSettingsClient: SettingsClient
    private val mLocationSettingsRequest: LocationSettingsRequest
    private val locationManager: LocationManager
    private val locationRequest: LocationRequest

    fun turnGPSOn(onGpsListener: onGpsListener?) {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            onGpsListener?.gpsStatus(true)
        } else {
            mSettingsClient
                .checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener((context as Activity)) {
                    //  GPS is already enabled, callback GPS status through listener
                    onGpsListener?.gpsStatus(true)
                }
                .addOnFailureListener(context) { e ->
                    val statusCode = (e as ApiException).statusCode
                    when (statusCode) {
                        // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                            val rae = e as ResolvableApiException
                            rae.startResolutionForResult(context, GPS_REQUEST)
                        } catch (sie: SendIntentException) {
                            Log.i(ContentValues.TAG, "PendingIntent unable to execute request.")
                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            val errorMessage = "Dezactivati modul avion daca este pornit!"
                            Log.e(ContentValues.TAG, errorMessage)
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
//                            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
//                            startActivity(context, intent, null)
//                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
//                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
//                            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
//                            context.startActivity(intent)
//                            checkSettingsHandler.postDelayed(checkSettingsRunnable, 1000)
                        }
                    }
                }
        }
    }

//    private val checkSettingsHandler = Handler()
//    private val checkSettingsRunnable: Runnable = object : Runnable {
//        override fun run() {
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//                return
//            }
//            if (isAccessGranted()) {
//                val i = Intent(context, MainActivity::class.java)
//                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
//                context.startActivity(i)
//                return
//            }
//            checkSettingsHandler.postDelayed(this, 200)
//        }
//    }
//
//    private fun isAccessGranted(): Boolean {
//        return true
//    }

    interface onGpsListener {
        fun gpsStatus(isGPSEnable: Boolean)
    }

    init {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        mSettingsClient = LocationServices.getSettingsClient(context)
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 10 * 1000.toLong()
        locationRequest.fastestInterval = 2 * 1000.toLong()
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        mLocationSettingsRequest = builder.build()
        builder.setAlwaysShow(true)

    }
}