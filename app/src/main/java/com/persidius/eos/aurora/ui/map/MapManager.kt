package com.persidius.eos.aurora.ui.map

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.offline.*
import com.persidius.eos.aurora.MainActivity
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MapManager(val activity: MainActivity, reset: Boolean = false) {

    private val mapToken = "pk.eyJ1IjoiYWxleG5pY3VsYSIsImEiOiJjazVsNG5tejAwMWZzM3FxaTZqZW1qeWIzIn0.a0suQh0bx3TQRT4oGIuNdA"
    private val logTag = "MapManager"
    private val regionNameSibiu = "sibiu"
    private val regionBoundsSibiu = listOf(LatLng(46.27, 24.93) /*Northeast*/, LatLng(45.50, 23.62)/*Southwest*/)
    private var offlineManager: OfflineManager
    private val offlineReady = MutableLiveData<Boolean>()

    init {
        Mapbox.getInstance(activity.applicationContext, mapToken)

        offlineManager = OfflineManager.getInstance(activity)
        offlineReady.observe(activity, Observer { isReady ->
            if (isReady) {
                downloadOfflineRegion()
            }
        })
        if (reset) {
            deleteAllOfflineRegions()
        } else {
            offlineReady.value = true
        }
    }

    private fun downloadOfflineRegion() {
        // Create a bounding box for the offline region
        val latLngBounds = LatLngBounds.Builder()
            .include(regionBoundsSibiu[0]) /* Northeast */
            .include(regionBoundsSibiu[1]) /* Southwest */
            .build()

        // Define the offline region
        val definition = OfflineTilePyramidRegionDefinition(Style.MAPBOX_STREETS, latLngBounds, 5.0, 14.0, activity.resources.displayMetrics.density)

        // Implementation that uses JSON for the offline region name.
        val metadata = try {
            val jsonObject = JSONObject()
            jsonObject.put(regionNameSibiu, "Romania")
            val json = jsonObject.toString()
            json.toByteArray(Charsets.UTF_8)
        } catch (exception: Exception) {
            Log.e(logTag, "Failed to encode metadata: " + exception.message)
            null
        }

        val start = LocalDateTime.now()
        Log.d(logTag, "Start download region $regionNameSibiu")
        // Create the region asynchronously
        offlineManager.createOfflineRegion(definition, metadata!!,
            object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(offlineRegion: OfflineRegion) {
                    offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
                    // Monitor the download progress using setObserver
                    offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {

                        override fun onStatusChanged(status: OfflineRegionStatus) {
                            // Calculate the download percentage
                            val percentage = if (status.requiredResourceCount >= 0)
                                100.0 * status.completedResourceCount / status.requiredResourceCount else 0.0
                            if (status.isComplete) {
                                val diff = ChronoUnit.SECONDS.between(start, LocalDateTime.now())
                                Log.d(logTag, "Region downloaded successfully in $diff seconds")
                            } else if (status.isRequiredResourceCountPrecise) {
//                                Log.d(logTag, percentage.toString())
                            }
                        }

                        override fun onError(error: OfflineRegionError) {
                            Log.e(logTag, "onError reason: " + error.reason + ", onError message: " + error.message)
                        }

                        override fun mapboxTileCountLimitExceeded(limit: Long) {
                            Log.e(logTag, "Mapbox tile count limit exceeded: $limit")
                        }
                    })
                }

                override fun onError(error: String) {
                    Log.e(logTag, "Error: $error")
                }
            })
    }

    private fun deleteAllOfflineRegions() {
        offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<out OfflineRegion>?) {
                if (offlineRegions != null) {
                    var count = offlineRegions.size
                    offlineRegions.forEach { region ->
                        val name = region.id
                        region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                            override fun onDelete() {
                                Log.i(logTag, "Region deleted, $name")
                                count--
                                if (count <= 0) {
                                    offlineReady.value = true
                                }
                            }

                            override fun onError(error: String) {
                                Log.e(logTag, "Error trying to delete region: $error")
                            }
                        })
                    }
                }
            }

            override fun onError(error: String?) {
            }
        })
    }

}